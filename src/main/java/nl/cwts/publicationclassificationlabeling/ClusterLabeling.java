package nl.cwts.publicationclassificationlabeling;

import java.util.ArrayList;

/**
 * Cluster labeling.
 * 
 * @author Nees Jan van Eck
 */
public class ClusterLabeling
{
    /**
     * Short label.
     */
    public String shortLabel;

    /**
     * Long label.
     */
    public String longLabel;

    /**
     * Keywords.
     */
    public ArrayList<String> keywords;

    /**
     * Summary.
     */
    public String summary;

    /**
     * Wikipedia page.
     */
    public String wikipediaPage;

    /**
     * Constructs a ClusterLabeling object.
     * 
     * @param shortLabel    Short label
     * @param longLabel     Long label
     * @param keywords      Keywords
     * @param summary       Summary
     * @param wikipediaPage Wikipedia page
     */
    public ClusterLabeling(String shortLabel, String longLabel, ArrayList<String> keywords, String summary, String wikipediaPage)
    {
        this.shortLabel = shortLabel;
        this.longLabel = longLabel;
        this.keywords = keywords;
        this.summary = summary;
        this.wikipediaPage = wikipediaPage;
    }

    /**
     * Returns the keyword list as a string.
     * 
     * @return Keywords
     */
    public String getKeywords()
    {
        if (keywords.isEmpty())
            return null;
        String keywords = "";
        for (int j = 0; j < this.keywords.size(); j++)
        {
            keywords += this.keywords.get(j);
            if (j < (this.keywords.size() - 1))
                keywords += "; ";
        }
        return keywords;
    }
}
