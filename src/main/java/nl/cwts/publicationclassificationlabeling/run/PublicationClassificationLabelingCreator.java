package nl.cwts.publicationclassificationlabeling.run;

import nl.cwts.publicationclassificationlabeling.ClusterLabeling;
import nl.cwts.publicationclassificationlabeling.GPTClusterLabeler;
import nl.cwts.publicationclassificationlabeling.GPTModel;

/**
 * Command line tool for obtaining labels for clusters of scientific publications.
 *
 * <p>
 * All methods in this class are static.
 * </p>
 *
 * @author Nees Jan van Eck
 */
public class PublicationClassificationLabelingCreator
{
    /**
     * Description text.
     */
    public static final String DESCRIPTION
        = "PublicationClassificationLabelingCreator version 1.0.0\n"
          + "By Nees Jan van Eck\n"
          + "Centre for Science and Technology Studies (CWTS), Leiden University\n";

    /**
     * Usage text.
     */
    public static final String USAGE
    = "Usage: PublicationClassificationLabelingCreator\n"
        + "\t<pub_titles_file> <label_file>\n"
        + "\t<api_key> <gpt_model> <print_labeling>\n"
        + "\t\t(to create a publication classification labeling based on data in text files)\n\n"
        + "   or  PublicationClassificationLabelingCreator\n"
        + "\t<server> <database> <pub_titles_table> <label_table>\n"
        + "\t<api_key> <gpt_model> <print_labeling>\n"
        + "\t\t(to create a publication classification labeling based on data in an SQL Server database)\n\n"
        + "Arguments:\n"
        + "<pub_titles_file>\n"
        + "\tName of the publication titles input file. This text file must contain two tab-separated \n"
        + "\tcolumns (without a header line): a column of cluster numbers and a column of publication \n"
        + "\ttitles. The cluster numbers in the first column must be integers starting at zero. The \n"
        + "\tpublication titles in the second column (e.g., the titles of a sample of 100 publications) \n"
        + "\tmust be concatenated into a single string. The lines in the file must be sorted by the \n"
        + "\tcluster numbers in the first column.\n"
        + "<label_file>\n"
        + "\tName of the labels output file. This text file will contain six tab-separated columns \n"
        + "\t(without a header line): a column of cluster numbers, a column of short labels, a column \n"
        + "\tof long labels, a column of keywords, a column of descriptions, and a column of Wikipedia \n"
        + "\tpage links. Cluster numbers are integers starting at zero.\n"
        + "<server>\n"
        + "\tSQL Server server name. A connection will be made using integrated authentication.\n"
        + "<database>\n"
        + "\tDatabase name.\n"
        + "<pub_titles_table>\n"
        + "\tName of the publication titles input table. This table must have two columns: cluster_no \n"
        + "\tand pub_titles. The cluster numbers in the first column must be integers starting at zero. \n"
        + "\tThe publication titles in the second column (e.g., the titles of a sample of 100 \n"
        + "\tpublications) must be concatenated into a single string.\n"
        + "<label_table>\n"
        + "\tName of the labels output table. This table will have six columns: cluster_no, \n"
        + "\tshort_label, long_label, keywords, summary, and wikipedia_url. Cluster numbers are \n"
        + "\tintegers starting at zero.\n"
        + "<api_key>\n"
        + "\tOpenAI API key.\n"
        + "<gpt_model>\n"
        + "\tOpenAI GPT model. The models supported are: 'gpt-4-1106-preview', 'gpt-4', \n"
        + "\t'gpt-3.5-turbo-1106', and 'gpt-3.5-turbo'.\n"
        + "<print_labeling>\n"
        + "\tBoolean indicating whether the generated publication classification labeling should be \n"
        + "\tprinted to the standard output or not.\n";

    /**
     * This method is called when the tool is started.
     *
     * @param args Command line arguments
     */
    public static void main(String[] args)
    {
        System.out.println(DESCRIPTION);
        if (args.length == 0)
        {
            System.out.print(USAGE);
            System.exit(-1);
        }

        // Process command line arguments.
        boolean useFiles = false;
        if (args.length == 5)
            useFiles = true;
        else if (args.length != 7)
        {
            System.err.print("Error while processing command line arguments: Incorrect number of command line arguments.\n\n" + USAGE);
            System.exit(-1);
        }

        String clusterPubTitlesFile = null;
        String clusterLabelingFile = null;
        String server = null;
        String database = null;
        String clusterPubTitlesTable = null;
        String clusterLabelingTable = null;
        String apiKey = null;
        GPTModel model = null;
        boolean printLabeling = true;
        int argIndex = 0;
        if (useFiles)
        {
            clusterPubTitlesFile = args[argIndex++];
            clusterLabelingFile = args[argIndex++];
        }
        else
        {
            server = args[argIndex++];
            database = args[argIndex++];
            clusterPubTitlesTable = args[argIndex++];
            clusterLabelingTable = args[argIndex++];
        }
        apiKey = args[argIndex++];
        try
        {
            model = GPTModel.getModel(args[argIndex++]);
            if (model == null)
                throw new IllegalArgumentException();
        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Error while processing command line argument <gpt_model>: Value must 'gpt-4-1106-preview', 'gpt-4', 'gpt-3.5-turbo-1106', or 'gpt-3.5-turbo'.\n\n" + USAGE);
            System.exit(-1);
        }
        try
        {
            if (!args[argIndex].equalsIgnoreCase("true") && !args[argIndex].equalsIgnoreCase("false"))
                throw new IllegalArgumentException();
            printLabeling = Boolean.parseBoolean(args[argIndex++]);

        }
        catch (IllegalArgumentException e)
        {
            System.err.println("Error while processing command line argument <print_labeling>: Value must be a boolean ('true' or 'false').\n\n" + USAGE);
            System.exit(-1);
        }

        // Read publication titles from file or database.
        System.out.print("Reading publication titles from " + ((useFiles) ? "file" : "database") + "... ");
        long startTimeReadPubTitles = System.currentTimeMillis();
        String[] clusterPubTitles;
        if (useFiles)
            clusterPubTitles = FileIO.readClusterPublicationTitles(clusterPubTitlesFile);
        else
            clusterPubTitles = DatabaseIO.readClusterPublicationTitles(server, database, clusterPubTitlesTable);
        System.out.println("Finished!");
        System.out.println("Reading publication titles from " + ((useFiles) ? "file" : "database") + " took " + formatDuration((System.currentTimeMillis() - startTimeReadPubTitles) / 1000) + ".");
        System.out.println();

        // Create labeling for each cluster.
        System.out.println("Creating labeling for each cluster...");
        System.out.println();
        long startCreateClusterLabeling = System.currentTimeMillis();
        GPTClusterLabeler clusterLabeler = new GPTClusterLabeler(apiKey, model);
        int nClusters = clusterPubTitles.length;
        ClusterLabeling[] clusterLabeling = new ClusterLabeling[nClusters];
        for (int i = 0; i < nClusters; i++)
        {
            int clusterNo = i;
            if ((clusterNo < nClusters) && !clusterPubTitles[clusterNo].isEmpty())
            {
                System.out.print("Creating labeling cluster " + clusterNo + "... ");
                clusterLabeling[clusterNo] = clusterLabeler.getClusterLabeling(clusterPubTitles[clusterNo]);
                System.out.println("Finished!");
                if (printLabeling)
                {
                    System.out.println("Labeling:");
                    System.out.println("\tShort label: " + clusterLabeling[clusterNo].shortLabel);
                    System.out.println("\tLong label:  " + clusterLabeling[clusterNo].longLabel);
                    System.out.println("\tKeywords:    " + clusterLabeling[clusterNo].getKeywords());
                    System.out.println("\tSummary:     " + clusterLabeling[clusterNo].summary);
                    System.out.println("\tWikipedia:   " + clusterLabeling[clusterNo].wikipediaPage);
                    System.out.println();
                }
            }
        }
        System.out.println("Creating labeling for each cluster took " + formatDuration((System.currentTimeMillis() - startCreateClusterLabeling) / 1000) + ".");
        System.out.println();

        // Write labeling to file or database.
        System.out.print("Writing labeling to " + ((useFiles) ? "file" : "database") + "... ");
        long startTimeWriteClusterLabeling = System.currentTimeMillis();
        if (useFiles)
            FileIO.writeClusterLabeling(clusterLabelingFile, clusterLabeling);
        else
            DatabaseIO.writeClusterLabelings(server, database, clusterLabelingTable, clusterLabeling);
        System.out.println("Finished!");
        System.out.println("Writing labeling to " + ((useFiles) ? "file" : "database") + " took " + formatDuration((System.currentTimeMillis() - startTimeWriteClusterLabeling) / 1000) + ".");
    }

    /**
     * Formats a given duration in seconds.
     *
     * @param s Duration in seconds
     * 
     * @return Formatted duration
     */
    private static String formatDuration(long s)
    {
        return String.format("%dh %dm %ds", s / 3600, (s % 3600) / 60, (s % 60));
    }
}
