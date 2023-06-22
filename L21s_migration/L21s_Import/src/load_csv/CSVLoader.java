package load_csv;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public class CSVLoader {

    /**
     * Loads CSV data from the CSV File under the specified URL into List of Strings
     *
     * @param csvUrl the URL of the CSV file
     * @return a list of string arrays representing the loaded CSV data
     */
    public List<String[]> loadCSVData(String csvUrl) {

        List<String[]> data = new ArrayList<>(); // List to store the loaded CSV data
        BufferedReader reader = null;

        try {

            URL url = new URL(csvUrl); // Create a URL instance with the specified CSV URL
            reader = new BufferedReader(new InputStreamReader(url.openStream()));
            String line;

            while ((line = reader.readLine()) != null) {
                String[] row = line.split(","); // Split the line based on the comma
                data.add(row); // Add the split row to the data list
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close(); // Close the BufferedReader object
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

        return data; // Return the loaded CSV data
    }
}
