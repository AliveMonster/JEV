package com.example.fraudpoc.dataset;

import com.example.fraudpoc.model.*;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

import java.io.File;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.*;

public class FraudDatasetGenerator {
    private static final int CUSTOMERS = 1000;
    private static final int TRANSACTIONS = 10000;
    private static final int FRAUD = 1000;
    private static final Random R = new Random(42);
    private static final List<String> CITIES = List.of("Bangalore","Mumbai","Delhi","Hyderabad","Chennai","Pune","Kolkata");
    private static final List<String> CATEGORIES = List.of("GROCERY","RESTAURANT","FUEL","UTILITIES","SHOPPING","TRAVEL","ELECTRONICS","PHARMACY","ENTERTAINMENT");
    private static final List<String> TYPES = List.of("UPI","CARD","BANK_TRANSFER","WALLET");

    public static void main(String[] args) throws Exception {
        String output = args.length > 0 ? args[0] : "src/main/resources/datasets/fraud-dataset.json";
        List<CustomerProfile> customers = generateCustomers();
        List<DatasetRecord> records = generateRecords(customers);
        ObjectMapper mapper = new ObjectMapper().registerModule(new JavaTimeModule()).enable(SerializationFeature.INDENT_OUTPUT);
        File file = new File(output);
        File parent = file.getParentFile();
        if (parent != null) parent.mkdirs();
        mapper.writeValue(file, records);
        System.out.printf("Generated %d records: %d safe, %d fraud -> %s%n", records.size(), records.stream().filter(r -> !r.actualFraud()).count(), records.stream().filter(DatasetRecord::actualFraud).count(), file.getAbsolutePath());
    }

    static List<CustomerProfile> generateCustomers() {
        List<CustomerProfile> list = new ArrayList<>();
        for (int i = 1; i <= CUSTOMERS; i++) {
            String id = String.format("CUST-%04d", i);
            String home = pick(CITIES);
            double avg = round(500 + R.nextDouble() * 4500);
            List<String> categories = new ArrayList<>(CATEGORIES);
            Collections.shuffle(categories, R);
            list.add(new CustomerProfile(id, money(avg), 1 + R.nextInt(6), 5 + R.nextInt(31), List.of(home), List.of(String.format("DEVICE-%04d", i)), categories.subList(0, 3)));
        }
        return list;
    }

    static List<DatasetRecord> generateRecords(List<CustomerProfile> customers) {
        List<DatasetRecord> records = new ArrayList<>();
        LocalDateTime start = LocalDateTime.of(2026, 9, 1, 8, 0);
        for (int i = 1; i <= TRANSACTIONS; i++) {
            CustomerProfile c = pick(customers);
            boolean fraud = i <= FRAUD;
            FraudPattern pattern = fraud ? fraudPattern() : FraudPattern.NONE;
            Transaction t = fraud ? fraudTx(i, c, pattern, start) : safeTx(i, c, start);
            records.add(new DatasetRecord(t, c, fraud, pattern));
        }
        Collections.shuffle(records, R);
        return records;
    }

    private static Transaction safeTx(int i, CustomerProfile c, LocalDateTime start) {
        double amount = Math.max(100, gaussian(c.averageTransactionAmount().doubleValue(), c.averageTransactionAmount().doubleValue() * .35));
        String home = c.usualLocations().get(0);
        String location = home;
        String device = c.usualDeviceIds().get(0);
        boolean newDevice = false;
        boolean newBeneficiary = false;
        String category = pick(c.usualMerchantCategories());
        double anomaly = R.nextDouble();
        if (anomaly < .12) amount = c.averageTransactionAmount().doubleValue() * (3 + R.nextDouble() * 5);
        else if (anomaly < .22) { newDevice = true; device = "DEVICE-NEW-" + (1000 + R.nextInt(9000)); }
        else if (anomaly < .30) location = differentCity(home);
        else if (anomaly < .38) category = pick(CATEGORIES);
        if (R.nextDouble() < .04) { newDevice = true; device = "DEVICE-NEW-" + (1000 + R.nextInt(9000)); location = differentCity(home); amount = c.averageTransactionAmount().doubleValue() * (2 + R.nextDouble() * 3); }
        return tx(i, c, start, amount, category, location, device, newDevice, newBeneficiary, R.nextInt(3), 1 + R.nextInt(6));
    }

    private static Transaction fraudTx(int i, CustomerProfile c, FraudPattern p, LocalDateTime start) {
        double amount = c.averageTransactionAmount().doubleValue();
        String home = c.usualLocations().get(0), location = home, device = c.usualDeviceIds().get(0), category = pick(c.usualMerchantCategories());
        boolean newDevice = false, newBeneficiary = false; int failed = 0, tx24 = c.transactionCount24h();
        switch (p) {
            case ACCOUNT_TAKEOVER -> { amount = c.averageTransactionAmount().doubleValue() * (8 + R.nextDouble()*22); newDevice=true; device="DEVICE-FRAUD-"+(1000+R.nextInt(9000)); newBeneficiary=true; location=differentCity(home); failed=3+R.nextInt(6); }
            case VELOCITY_FRAUD -> { amount = c.averageTransactionAmount().doubleValue() * (1 + R.nextDouble()*4); tx24=10+R.nextInt(11); }
            case HIGH_VALUE_ANOMALY -> { amount = c.averageTransactionAmount().doubleValue() * (15 + R.nextDouble()*35); location=differentCity(home); }
            case NEW_BENEFICIARY -> { amount=c.averageTransactionAmount().doubleValue()*(5+R.nextDouble()*15); newBeneficiary=true; if(R.nextDouble()<.65){newDevice=true;device="DEVICE-FRAUD-"+(1000+R.nextInt(9000));} }
            case AUTHENTICATION_ATTACK -> { amount=c.averageTransactionAmount().doubleValue()*(3+R.nextDouble()*9); failed=5+R.nextInt(8); if(R.nextDouble()<.7){newDevice=true;device="DEVICE-FRAUD-"+(1000+R.nextInt(9000));} }
            case MERCHANT_ANOMALY -> { amount=c.averageTransactionAmount().doubleValue()*(2+R.nextDouble()*8); List<String> other=CATEGORIES.stream().filter(x->!c.usualMerchantCategories().contains(x)).toList(); category=pick(other); newBeneficiary=R.nextBoolean(); }
            default -> {}
        }
        return tx(i,c,start,amount,category,location,device,newDevice,newBeneficiary,failed,tx24);
    }

    private static Transaction tx(int i, CustomerProfile c, LocalDateTime start, double amount, String category, String location, String device, boolean newDevice, boolean newBeneficiary, int failed, int tx24) {
        return new Transaction(String.format("TXN-%06d", i), c.customerId(), start.plusMinutes(i*7L + R.nextInt(7)-3), money(amount), "INR", pick(TYPES), category, String.format("MERCHANT-%04d", 1+R.nextInt(500)), location, device, newDevice, newBeneficiary, failed, tx24);
    }
    private static FraudPattern fraudPattern(){ return pick(List.of(FraudPattern.ACCOUNT_TAKEOVER,FraudPattern.VELOCITY_FRAUD,FraudPattern.HIGH_VALUE_ANOMALY,FraudPattern.NEW_BENEFICIARY,FraudPattern.AUTHENTICATION_ATTACK,FraudPattern.MERCHANT_ANOMALY)); }
    private static String differentCity(String current){ String x; do{x=pick(CITIES);}while(x.equals(current)); return x; }
    private static double gaussian(double mean,double sd){return mean+R.nextGaussian()*sd;}
    private static BigDecimal money(double v){return BigDecimal.valueOf(round(Math.max(0,v))).setScale(2, RoundingMode.HALF_UP);}
    private static double round(double v){return Math.round(v*100.0)/100.0;}
    private static <T>T pick(List<T> x){return x.get(R.nextInt(x.size()));}
}
