package com.example.urban;
import android.content.Context;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class DataStore {
    private static String filename;
    private static String filepath;
    static File file;
    private BufferedWriter fos;
    private static Context context;
    private static Writer writer;
    public DataStore(Context mainContext){
        context = mainContext;
    }

    public void open() throws IOException {
        SimpleDateFormat simpleDateFormat = new SimpleDateFormat("yyyyMMdd_HHmmss");
        Date date = new Date(System.currentTimeMillis());
        filename = "Sensor_" + simpleDateFormat.format(date) + ".csv";
        filepath = context.getExternalFilesDir(null)+"/"+filename;
        System.out.println(filepath);
        file = new File(filepath);
        if (!file.exists()) {
            try {
                file.createNewFile();
            }
            catch (IOException e) {
                e.printStackTrace();
            }
            writer = new BufferedWriter(new FileWriter(file, true));
        }
        try {
            fos = new BufferedWriter(new FileWriter(file,true));
            fos.flush();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    public void close(){
        try {
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void writeCSVData(float[] sensors, double[] gnssdata, double[] nmeadata, float t) throws IOException {
       for (int i=0; i<10; i++)  fos.write(String.valueOf(sensors[i])+','+'\n');fos.flush();
        for (int j = 0; j < 4; j++) {
            fos.write(String.valueOf(gnssdata[j])+','+String.valueOf(nmeadata[j])+'\n');
            fos.flush();
        }
    }
}
