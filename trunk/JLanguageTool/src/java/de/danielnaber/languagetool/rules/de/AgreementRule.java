/* LanguageTool, a natural language style checker 
 * Copyright (C) 2005 Daniel Naber (http://www.danielnaber.de)
 * 
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this library; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA  02110-1301
 * USA
 */
package de.danielnaber.languagetool.rules.de;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.ResourceBundle;
import java.util.Set;

import de.danielnaber.languagetool.AnalyzedSentence;
import de.danielnaber.languagetool.AnalyzedTokenReadings;
import de.danielnaber.languagetool.JLanguageTool;
import de.danielnaber.languagetool.rules.Category;
import de.danielnaber.languagetool.rules.RuleMatch;
import de.danielnaber.languagetool.tagging.de.AnalyzedGermanToken;
import de.danielnaber.languagetool.tagging.de.AnalyzedGermanTokenReadings;
import de.danielnaber.languagetool.tagging.de.GermanToken.POSType;

/**
 * Simple agreement checker for German noun phrases. Checks agreement in:
 * 
 * <ul>
 *  <li>DET/PRO NOUN: e.g. "mein Auto", "der Mann", "die Frau" (correct), "die Haus" (incorrect)</li>
 *  <li>DET/PRO ADJ NOUN: e.g. "der riesige Tisch" (correct), "die riesigen Tisch" (incorrect)</li> 
 * </ul>
 * 
 * Note that this rule only checks agreement inside the noun phrase, not whether
 * e.g. the correct case is used. For example, "Es ist das Haus dem Mann" is not
 * detected as incorrect. 
 *  
 * @author Daniel Naber
 */
public class AgreementRule extends GermanRule {

  public AgreementRule(final ResourceBundle messages) {
    if (messages != null)
      super.setCategory(new Category(messages.getString("category_grammar")));
  }
  
  public String getId() {
    return "DE_AGREEMENT";
  }

  public String getDescription() {
    return "Kongruenz von Nominalphrasen (unvollständig!), z.B. 'mein kleiner(kleines) Haus'";
  }

  public RuleMatch[] match(final AnalyzedSentence text) {
    List<RuleMatch> ruleMatches = new ArrayList<RuleMatch>();
    AnalyzedTokenReadings[] tokens = text.getTokensWithoutWhitespace();
    int pos = 0;
    for (int i = 0; i < tokens.length; i++) {
    //defaulting to the first reading
    //TODO: check for all readings
    //and replace GermanTokenReading
      String posToken = tokens[i].getAnalyzedToken(0).getPOSTag();
      if (posToken != null && posToken.equals(JLanguageTool.SENTENCE_START_TAGNAME))
        continue;
      //AnalyzedGermanToken analyzedToken = new AnalyzedGermanToken(tokens[i]);
      
    	AnalyzedGermanTokenReadings analyzedToken = (AnalyzedGermanTokenReadings)tokens[i];
        boolean isRelevantPronomen = analyzedToken.hasReadingOfType(POSType.PRONOMEN);     
      // avoid false alarms:
      if (i > 0 && tokens[i-1].getToken().equalsIgnoreCase("vor") && tokens[i].getToken().equalsIgnoreCase("allem"))
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("er") || tokens[i].getToken().equalsIgnoreCase("sie") || tokens[i].getToken().equalsIgnoreCase("es"))
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("dessen"))      // avoid false alarm on: "..., dessen Leiche"
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("deren"))
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("sich"))      // avoid false alarm
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("unser"))      // avoid false alarm "unser Produkt": TODO!
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("aller"))
        isRelevantPronomen = false;
      else if (tokens[i].getToken().equalsIgnoreCase("man"))
        isRelevantPronomen = false;
     
      // avoid false alarm: "Das Wahlrecht, das Frauen zugesprochen bekamen.":
      boolean ignore = tokens[i-1].getToken().equals(",") && tokens[i].getToken().equalsIgnoreCase("das");
      if ((analyzedToken.hasReadingOfType(POSType.DETERMINER) || isRelevantPronomen) && !ignore) {
        int tokenPos = i + 1; 
        if (tokenPos >= tokens.length)
          break;
        AnalyzedGermanTokenReadings nextToken = (AnalyzedGermanTokenReadings)tokens[tokenPos];
        if (nextToken.hasReadingOfType(POSType.ADJEKTIV)) {
          // TODO: Berliner is also an adjective in out Morphy dictionary, others (Münchner etc) 
          // are not. Without this special case "In seiner Berliner Rede" is flagged as incorrect:
          if ("Berliner".equals(nextToken.getToken()))
            break;
          tokenPos = i + 2; 
          if (tokenPos >= tokens.length)
            break;
          AnalyzedGermanTokenReadings nextNextToken = (AnalyzedGermanTokenReadings)tokens[tokenPos];
          if (nextNextToken.hasReadingOfType(POSType.NOMEN)) {
            // TODO: add a case (checkAdjNounAgreement) for special cases like "deren",
            // e.g. "deren komisches Geschenke" isn't yet detected as incorrect
            RuleMatch ruleMatch = checkDetAdjNounAgreement((AnalyzedGermanTokenReadings)tokens[i],
                (AnalyzedGermanTokenReadings)tokens[i+1], (AnalyzedGermanTokenReadings)tokens[i+2]);
            if (ruleMatch != null)
              ruleMatches.add(ruleMatch);
          }
        } else if (nextToken.hasReadingOfType(POSType.NOMEN)) {
          RuleMatch ruleMatch = checkDetNounAgreement((AnalyzedGermanTokenReadings)tokens[i],
              (AnalyzedGermanTokenReadings)tokens[i+1]);
          if (ruleMatch != null)
            ruleMatches.add(ruleMatch);
        }
      }
     
      pos += tokens[i].getToken().length();
    }
    return toRuleMatchArray(ruleMatches);
  }

  private RuleMatch checkDetNounAgreement(final AnalyzedGermanTokenReadings token1,
      final AnalyzedGermanTokenReadings token2) {
    RuleMatch ruleMatch = null;
    Set<String> set1 = getAgreementCategories(token1);
    if (set1 == null)
      return null;  // word not known, assume it's correct
    Set<String> set2 = getAgreementCategories(token2);
    if (set2 == null)
      return null;
    set1.retainAll(set2);
    if (set1.size() == 0) {
      // TODO: better error message than just 'agreement error'
      String msg = "Möglicherweise fehlende Übereinstimmung (Kongruenz) zwischen Artikel und Nomen " +
            "bezüglich Kasus, Numerus oder Genus, z.B. 'meine Haus' statt 'mein Haus'";
      ruleMatch = new RuleMatch(this, token1.getStartPos(), 
          token2.getStartPos()+token2.getToken().length(), msg);
    }
    return ruleMatch;
  }

  private RuleMatch checkDetAdjNounAgreement(final AnalyzedGermanTokenReadings token1,
      final AnalyzedGermanTokenReadings token2, final AnalyzedGermanTokenReadings token3) {
    RuleMatch ruleMatch = null;
    Set<String> set1 = getAgreementCategories(token1);
    if (set1 == null)
      return null;  // word not known, assume it's correct
    //Set set1Orig = getAgreementCategories(term1);
    Set<String> set2 = getAgreementCategories(token2);
    if (set2 == null)
      return null;
    Set<String> set3 = getAgreementCategories(token3);
    if (set3 == null)
      return null;
    set1.retainAll(set2);
    set1.retainAll(set3);
    if (set1.size() == 0) {          
      String msg = "Fehlende Übereinstimmung (Kongruenz) zwischen Artikel, Adjektiv und" +
            " Nomen bezüglich Kasus, Numerus oder Genus.";
      ruleMatch = new RuleMatch(this, token1.getStartPos(), 
          token3.getStartPos()+token3.getToken().length(), msg);
    }
    return ruleMatch;
  }

  /** Return Kasus, Numerus, Genus. */
  private Set<String> getAgreementCategories(final AnalyzedGermanTokenReadings aToken) {
    Set<String> set = new HashSet<String>();
    List<AnalyzedGermanToken> readings = aToken.getGermanReadings();
    for (AnalyzedGermanToken reading : readings) {
      if (reading.getCasus() == null && reading.getNumerus() == null &&
          reading.getGenus() == null)
        continue;
      set.add(reading.getCasus() + "/" + reading.getNumerus()
          + "/" + reading.getGenus());
    }
    return set;
  }

  public void reset() {
  }

}
