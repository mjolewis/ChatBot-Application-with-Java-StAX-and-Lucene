package edu.bu.met622;

import edu.bu.met622.database.MySQL;
import edu.bu.met622.searchlib.SearchEngine;
import edu.bu.met622.sharedresources.Constants;
import edu.bu.met622.utils.FileMerger;
import edu.bu.met622.utils.XMLParser;

import javax.swing.*;
import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.sql.*;
import java.util.ArrayList;
import java.util.Scanner;

/**********************************************************************************************************************
 * Mediator class to build the simulation
 *
 * @author Michael Lewis
 *********************************************************************************************************************/
public class Builder {
    private ArrayList<Long> bruteForceRunTimes;
    private ArrayList<Long> luceneRunTimes;

    /**
     * Initialize a new Builder
     *
     * @throws OutOfMemoryError Indicates insufficient memory for this Builder
     */
    public Builder() {
        bruteForceRunTimes = new ArrayList<>();
        luceneRunTimes = new ArrayList<>();
    }

    /**
     * Gets files using a file chooser dialog box and builds the simulation. This method ensures that the user has
     * selected more than one file to merge
     */
    public void build() {
        JFileChooser jFileChooser = new JFileChooser(FileSystemView.getFileSystemView());
        jFileChooser.setMultiSelectionEnabled(true);

        File[] selectedFiles;
        while (true) {
            int returnValue = jFileChooser.showOpenDialog(null);

            if (returnValue == JFileChooser.APPROVE_OPTION) {
                selectedFiles = jFileChooser.getSelectedFiles();

                if (selectedFiles.length == 1) {                         // Only merge if more than one was selected
                    System.out.println(Constants.FILE_SELECTION_ERROR);
                } else {
                    mergeXML(selectedFiles);
                    parseXML();
                    break;
                }
            } else {                                                     // No files selected
                System.out.println(Constants.FILE_SELECTION_ERROR);

                if (returnValue == JFileChooser.CANCEL_OPTION) {         // Break out of the selection process
                    break;
                }
            }

        }
    }

    public void buildDB() {
        MySQL db = MySQL.getInstance();

        // Connect to the database
        System.out.println(Constants.CONNECTING);
        Connection connection = db.getConnection();
        System.out.println(Constants.CONNECTED);

        try {
            Statement statement = connection.createStatement();
            // TODO: 10/24/20 do i need to build that database after power off e.g. met622
            // Create table if it doesn't exist
            DatabaseMetaData metaData = connection.getMetaData();
            //ResultSet table = metaData.getTables(null, null, Constants.TABLE_NAME, null);
            if (true) {
                System.out.println(Constants.CREATING_TABLE);
                statement.execute(Constants.CREATE_TABLE);
                System.out.println(Constants.CREATED_TABLE);
            } else {
                ResultSet rs = statement.executeQuery("SELECT * FROM articles");
                while (rs.next()) {
                    //statement.execute(Constants.CREATE_TABLE);
                    System.out.println(rs.getString(1));
                }
            }

        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    /**
     * Write a welcome message to the console
     */
    public void startMessage() {
        System.out.println("\n\n************************************************");
        System.out.println(Constants.START_MESSAGE);
        System.out.println("\n************************************************\n");
    }

    /**
     * Write an ending message to the console
     */
    public void endMessage() {
        System.out.println("\n\n************************************************");
        System.out.println(Constants.END_MESSAGE);
        System.out.println("\n************************************************");
    }

    //*****************************************************************************************************************
    // Helper methods
    //*****************************************************************************************************************

    /*
     * Merge multiple XML documents into one
     */
    private void mergeXML(File[] selectedFiles) {
        ArrayList<String> fileContainer = new ArrayList<>();
        for (File file : selectedFiles) {
            fileContainer.add(file.getPath());
        }

        FileMerger fileMerger = new FileMerger(fileContainer, Constants.OUTPUT_FILE);
        try {
            fileMerger.merge();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /*
     * Search an XML document for a search parameter specified by the user
     */
    private void parseXML() {
        Scanner scanner = new Scanner(System.in);
        XMLParser XMLParser = new XMLParser();
        SearchEngine searchEngine = new SearchEngine();
        boolean isParsed = false;

        do {

            if ("0".equals(getSearchType(scanner))) {                     // Brute force search
                isParsed = true;

                // Perform search and keep track of the runtime
                bruteForceRunTimes.add(XMLParser.parse(getSearchParam(scanner), getNumOfDocs(scanner)));

                if ("y".equalsIgnoreCase(displaySearchHistory(scanner))) { XMLParser.print(); }
            } else {
                if (!isParsed) {                                          // Lucene Index
                    isParsed = true;
                    XMLParser.parse();
                }

                // Perform search and keep track of the runtime
                luceneRunTimes.add(searchEngine.search(getSearchParam(scanner), getNumOfDocs(scanner)));

                if ("y".equalsIgnoreCase(displayDocuments(scanner))) { searchEngine.displayHits(); }
            }

            if ("y".equalsIgnoreCase(displaySearchTimes(scanner))) { printSearchTimes(); }

        } while ("y".equalsIgnoreCase(performSearch(scanner)));
    }

    /*
     * Gets the search type from the user. If the user enters 0, then a brute force search is performed. Otherwise, a
     * Lucene Index search is performed
     */
    private String getSearchType(Scanner scanner) {
        System.out.println("\nEnter search type");
        System.out.print("0 for Brute Force; 1 for Lucene: ");
        return scanner.nextLine();
    }

    /*
     * Gets the search parameter from the user
     */
    private String getSearchParam(Scanner scanner) {
        System.out.print("Enter search parameter: ");
        return scanner.nextLine();
    }

    /*
     * Gets the number of documents to return from the document search
     */
    private int getNumOfDocs(Scanner scanner) {
        while (true) {
            System.out.print("Number of docs: ");
            try {
                return Integer.parseInt(scanner.nextLine());
            } catch (NumberFormatException e) {
                System.out.print("Invalid input. Try Again.");
            }
        }
    }

    /*
     * Ask the user whether or not to display documents from the Lucene search
     */
    private String displayDocuments(Scanner scanner) {
        System.out.print("Display documents: (y/n)? ");
        return scanner.nextLine();
    }

    private String displaySearchTimes(Scanner scanner) {
        System.out.print("Display run times: (y/n)? ");
        return scanner.nextLine();
    }

    private void printSearchTimes() {

        System.out.println("Run time results for Brute force: ");
        for (Long runTime : bruteForceRunTimes) {
            System.out.println(runTime/Constants.MILLIS_TO_SECONDS + " seconds");
        }

        System.out.println("Run time results for Lucene search: ");
        for (Long runTime : luceneRunTimes) {
            System.out.println(runTime/Constants.MILLIS_TO_SECONDS + " seconds");
        }
    }

    /*
     * Ask the user whether or not to display search history
     */
    private String displaySearchHistory(Scanner scanner) {
        System.out.print("Display search history (y/n)? ");
        return scanner.nextLine();
    }

    /*
     * Ask the user whether or not to do another search
     */
    private String performSearch(Scanner scanner) {
        System.out.print("Search again (y/n)? ");
        return scanner.nextLine();
    }
}
