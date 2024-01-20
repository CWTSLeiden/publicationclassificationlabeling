package nl.cwts.publicationclassificationlabeling.run;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

import nl.cwts.publicationclassificationlabeling.ClusterLabeling;

public class DatabaseIO
{
    /**
     * Reads cluster publication titles from an SQL Server database table.
     *
     * @param server                SQL Server server name
     * @param database              Database name
     * @param clusterPubTitlesTable Name of the cluster publication titles table
     *
     * @return Cluster publication titles
     */
    public static String[] readClusterPublicationTitles(String server, String database, String clusterPubTitlesTable)
    {
        String[] clusterPubTitles = null;
        
        Connection connection = null;
        try
        {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ":1433;databaseName=" + database + ";integratedSecurity=true;encrypt=true;trustServerCertificate=true;");

            // Read number of clusters and publication titles.
            Statement statement = connection.createStatement();
            ResultSet resultSet = statement.executeQuery("select count(*), max(cluster_no) from " + clusterPubTitlesTable);
            resultSet.next();
            int nClusters = resultSet.getInt(1);
            int maxClusterNo = resultSet.getInt(2);
            statement.close();

            clusterPubTitles = new String[maxClusterNo + 1];
            
            // Read cluster publication titles.
            statement = connection.createStatement();
            resultSet = statement.executeQuery("select cluster_no, pub_titles from " + clusterPubTitlesTable + " order by cluster_no");
            for (int i = 0; i < nClusters; i++)
            {
                resultSet.next();
                int clusterNo = resultSet.getInt(1);
                String pubTitles = resultSet.getString(2);
                if (clusterPubTitles[clusterNo] != null)
                    throw new SQLException("Cluster number " + clusterNo + " occurs multiple times.");
                clusterPubTitles[clusterNo] = pubTitles;
            }

            connection.close();
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Error while reading from database: SQL Server Driver not found.");
            System.exit(-1);
        }
        catch (SQLException e)
        {
            System.err.println("Error while reading from database: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (connection != null)
                try
                {
                    connection.close();
                }
                catch (SQLException e)
                {
                    System.err.println("Error while reading from database: " + e.getMessage());
                    System.exit(-1);
                }
        }

        return clusterPubTitles;
    }

    /**
     * Writes a publication classification labeling to an SQL Server database table.
     *
     * @param server               SQL Server server name
     * @param database             Database name
     * @param clusterLabelingTable Name of the cluster labeling table
     * @param clusterLabeling      Cluster labelings
     */
    public static void writeClusterLabelings(String server, String database, String clusterLabelingTable, ClusterLabeling[] clusterLabeling)
    {
        Connection connection = null;
        try
        {
            Class.forName("com.microsoft.sqlserver.jdbc.SQLServerDriver");
            connection = DriverManager.getConnection("jdbc:sqlserver://" + server + ":1433;databaseName=" + database + ";integratedSecurity=true;encrypt=true;trustServerCertificate=true;");

            Statement statement = connection.createStatement();
            statement.executeUpdate("drop table if exists " + clusterLabelingTable);
            statement.executeUpdate("create table " + clusterLabelingTable + "(cluster_no smallint not null, short_label varchar(500) not null, long_label varchar(1000) not null, keywords varchar(max) not null, summary varchar(max) not null, wikipedia_url varchar(200) not null)");
            statement.close();
            
            int nClusters = clusterLabeling.length;
            for (int i = 0; i < nClusters; i++)
                if (clusterLabeling[i] != null)
                {
                    PreparedStatement preparedStatement = connection.prepareStatement("insert into " + clusterLabelingTable + " values (?, ?, ?, ?, ?, ?)");
                    preparedStatement.setInt(1, i);
                    preparedStatement.setString(2, clusterLabeling[i].shortLabel);
                    preparedStatement.setString(3, clusterLabeling[i].longLabel);
                    preparedStatement.setString(4, clusterLabeling[i].getKeywords());
                    preparedStatement.setString(5, clusterLabeling[i].summary);
                    preparedStatement.setString(6, clusterLabeling[i].wikipediaPage);
                    preparedStatement.execute();
                    preparedStatement.close();
                }

            connection.close();
        }
        catch (ClassNotFoundException e)
        {
            System.err.println("Error while writing publication classification labeling to database: SQL Server Driver not found.");
            System.exit(-1);
        }
        catch (SQLException e)
        {
            System.err.println("Error while writing publication classification labeling to database: " + e.getMessage());
            System.exit(-1);
        }
        finally
        {
            if (connection != null)
                try
                {
                    connection.close();
                }
                catch (SQLException e)
                {
                    System.err.println("Error while writing publication classification labeling to database: " + e.getMessage());
                    System.exit(-1);
                }
        }
    }
}
