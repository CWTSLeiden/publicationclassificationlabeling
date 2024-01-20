package nl.cwts.publicationclassificationlabeling.run;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;

import nl.cwts.publicationclassificationlabeling.ClusterLabeling;

public class FileIO
{
    /**
     * Column separator for input and output files.
     */
    public static final String COLUMN_SEPARATOR = "\t";

    /**
     * Reads cluster publication titles from a file.
     *
     * @param clusterPubTitlesFile Name of the cluster publication titles files
     *
     * @return Cluster publication titles
     */
    public static String[] readClusterPublicationTitles(String clusterPubTitlesFile)
    {
        String[] clusterPubTitles = null;

        BufferedReader reader = null;
        try
        {
            reader = new BufferedReader(new FileReader(clusterPubTitlesFile));
            int nLines = 0;
            while (reader.readLine() != null)
                nLines++;
            reader.close();
            if (nLines == 0)
                throw new IOException("File is empty.");

            clusterPubTitles = new String[nLines];

            reader = new BufferedReader(new FileReader(clusterPubTitlesFile));
            String line = reader.readLine();
            int lineNo = 0;
            while (line != null)
            {
                lineNo++;
                String[] columns = line.split(COLUMN_SEPARATOR);
                if (columns.length != 2)
                    throw new IOException("Incorrect number of columns (line " + lineNo + ").");
                int clusterNo;
                try
                {
                    clusterNo = Integer.parseUnsignedInt(columns[0]);
                }
                catch (NumberFormatException e)
                {
                    throw new IOException("Cluster numbers must be integers starting at zero (line " + lineNo + ").");
                }
                String pubTitles = columns[1];
                if (clusterPubTitles[clusterNo] != null)
                    throw new IOException("Cluster number " + clusterNo + " occurs multiple times (line " + lineNo + ").");
                clusterPubTitles[clusterNo] = pubTitles;
                line = reader.readLine();
            }
            reader.close();
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error while reading from file: File not found.");
            System.exit(-1);
        }
        catch (IOException e)
        {
            System.err.println("Error while reading from file: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (reader != null)
                try
                {
                    reader.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error while reading from file: " + e.getMessage());
                    System.exit(-1);
                }
        }

        return clusterPubTitles;
    }

    /**
     * Writes a publication classification labeling to a file.
     *
     * @param clusterLabelingFile Name of the cluster labeling file
     * @param clusterLabeling     Cluster labelings
     */
    public static void writeClusterLabeling(String clusterLabelingFile, ClusterLabeling[] clusterLabeling)
    {
        BufferedWriter writer = null;
        try
        {
            writer = new BufferedWriter(new FileWriter(clusterLabelingFile));
            int nClusters = clusterLabeling.length;
            for (int i = 0; i < nClusters; i++)
                if (clusterLabeling[i] != null)
                {
                    writer.write(i + "");
                    writer.write(COLUMN_SEPARATOR + clusterLabeling[i].shortLabel);
                    writer.write(COLUMN_SEPARATOR + clusterLabeling[i].longLabel);
                    writer.write(COLUMN_SEPARATOR + clusterLabeling[i].getKeywords());
                    writer.write(COLUMN_SEPARATOR + clusterLabeling[i].summary);
                    writer.write(COLUMN_SEPARATOR + clusterLabeling[i].wikipediaPage);
                    writer.newLine();
                }
        }
        catch (FileNotFoundException e)
        {
            System.err.println("Error while writing publication classification labeling to file: File not found.");
            System.exit(-1);
        }
        catch (IOException e)
        {
            System.err.println("Error while writing publication classification labeling to file: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (writer != null)
                try
                {
                    writer.close();
                }
                catch (IOException e)
                {
                    System.err.println("Error while writing publication classification labeling to file: " + e.getMessage());
                    System.exit(-1);
                }
        }
    }
}
