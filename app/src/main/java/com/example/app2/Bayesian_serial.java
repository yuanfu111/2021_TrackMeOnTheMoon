package com.example.app2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class Bayesian_serial {
    public int locate_cell(String rss_mac, float rss, List<String> macs, List<List<List<Float>>> mac_tables) {
        // Initialization
        int numCell = 9;
        List<Float> initial_belief = new ArrayList<>();
        for (int i=0; i<numCell; ++i) {
            initial_belief.add((float) (1.0/numCell));
        }
        List<Float> prior = initial_belief;
        List<Float> posterior = sense_serial(prior, rss_mac, rss, macs, mac_tables);
        // Calculate posterior iteratively until convergence
        float max_new = Collections.max(posterior);
        float max_old = 0;
        float delta = (float) 0.001;
        while((max_new < 0.9) & (max_new-max_old > delta) ) {
            prior = posterior;
            posterior = sense_serial(prior, rss_mac, rss, macs, mac_tables);
            max_old = max_new;
            max_new = Collections.max(posterior);
        }
        // The index of the max value in posterior represents the current cell
        return posterior.indexOf(max_new);
    }

    // One update on posterior
    public List<Float> sense_serial(List<Float> prior, String rss_mac, float rss, List<String> macs, List<List<List<Float>>> mac_tables) {
        List<Float> posterior = new ArrayList<>();
        // Find the table according to mac address
        int index = macs.indexOf(rss_mac);
        List<List<Float>> table = mac_tables.get(index);
        // Find the column in the table according to rss
        int col = 100 + (int)rss;
        // Calculate the posterior values and their sum
        float sum = 0;
        for (int row=0; row < prior.size(); ++row) {
            float value = prior.get(row) * table.get(row).get(col);
            sum += value;
            posterior.add(value);
        }
        // Normalize the posterior
        for (int row=0; row<prior.size(); ++row){
            posterior.set(row, posterior.get(row)/sum);
        }
        return posterior;
    }
}
