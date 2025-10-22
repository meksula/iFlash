package com.iflash.platform.maintenance;

import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class CsvCompanyReader {

    public List<Company> read(String path) {
        if (path == null || path.isEmpty()) {
            log.error("Empty initial ticker list!");
            return List.of();
        }
        List<Company> companies = new ArrayList<>();

        try (BufferedReader br = new BufferedReader(new InputStreamReader(getClass().getResourceAsStream(path)))) {
            String line;
            boolean firstLine = true;
            while ((line = br.readLine()) != null) {
                if (firstLine) {
                    firstLine = false;
                    continue;
                }
                String[] parts = line.split(",");
                String ticker = parts[1].trim();
                String price = parts[3].trim();

                companies.add(new Company(ticker, new BigDecimal(price)));
            }
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return companies;
    }
}
