import java.io.*;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.text.NumberFormat;
import java.text.ParseException;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.zip.GZIPInputStream;

public class DatabaseConnection
{
    public static final String host = "http://145.48.203.28/";

    static
    {
        try {
            Files.createDirectories(Paths.get(getCacheDir()));
            cleanCache();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static RawMeasurement getMostRecentMeasurement()
    {
        return buildMeasurement(new DataInputStream(buildReader(host + "last")));
    }

    public static void clearCache()
    {
        try {
            Files.list(Paths.get(getCacheDir())).forEach(file ->
            {
                try {
                    Files.delete(file);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            });


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    private static void cleanCache()
    {
        try {
            Files.list(Paths.get(getCacheDir())).forEach(file ->
            {
                try {
                    int timestamp = ((Number) NumberFormat.getInstance().parse(file.getFileName().toString())).intValue();
                    long current = (System.currentTimeMillis()/(1000*60*60));
                    if(timestamp != current)
                        Files.delete(file);

                } catch (ParseException e) {

                } catch (IOException e) {
                    e.printStackTrace();
                }
            });


        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static ArrayList<RawMeasurement> getMeasurementsBetween(LocalDateTime begin, LocalDateTime end)
    {
        return getMeasurements(host + "between/" + begin + "/" + end);
    }

    public static ArrayList<RawMeasurement> getMeasurementsSince(LocalDateTime since)
    {
        return getMeasurements(host + "between/"+since+"/" + LocalDateTime.now());
    }

    public static ArrayList<RawMeasurement> getMeasurementsLastYear()
    {
        return getMeasurements(host + "lastmonths/12");
    }

    public static ArrayList<RawMeasurement> getMeasurementsLastMonth() { return getMeasurementsLastMonths(1); }
    public static ArrayList<RawMeasurement> getMeasurementsLastMonths(int months)
    {
        return getMeasurements(host + "lastmonths/" + months + "/bin");
    }


    public static ArrayList<RawMeasurement> getMeasurementsLastDay() { return getMeasurementsLastDays(1); }
    public static ArrayList<RawMeasurement> getMeasurementsLastDays(int days)
    {
        return getMeasurements(host + "lastdays/" + days);
    }

    public static ArrayList<RawMeasurement> getMeasurementsLastHour() { return getMeasurementsLastHours(1); }
    public static ArrayList<RawMeasurement> getMeasurementsLastHours(int hours)
    {
        return getMeasurements(host + "lasthours/" + hours);
    }

    /**
     * Builds up a list of measurements
     * @param address the address (http or https protocol included) to call the REST API at
     * @return a list of raw measurements
     */
    private static ArrayList<RawMeasurement> getMeasurements(String address)
    {
        String cacheFile = getCacheDir() + buildCacheFileName(address) + ".bin";
        if(Files.exists(Paths.get(cacheFile))) {
            try {
                return (ArrayList<RawMeasurement>)new ObjectInputStream(new BufferedInputStream(new FileInputStream(cacheFile))).readObject();
            } catch (IOException e) {
                clearCache();
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        }
        try {

            InputStream is = buildReader(address);
            DataInputStream reader = new DataInputStream(is);

            ArrayList<RawMeasurement> measurements = new ArrayList<>();
            while(true) {
                RawMeasurement measurement = buildMeasurement(reader);
                if(measurement == null)
                    break;
                measurements.add(measurement);
            }
            new ObjectOutputStream(new BufferedOutputStream(new FileOutputStream(cacheFile))).writeObject(measurements);
            return measurements;
        }catch(Exception e)
        {
            e.printStackTrace();
        }
        return null;
    }


    private static String getCacheDir()
    {
        return System.getProperty("java.io.tmpdir") + "ws/";
    }
    private static String buildCacheFileName(String address)
    {
        address = address.substring(host.length());
        address = address.replace('/', '_');
        if(address.isEmpty())
            return "";

        address = (System.currentTimeMillis()/(1000*60*60)) + address;

        return address;
    }



    /**
     * Builds up a reader for an url. Handles gzip compression if available( it should be available)
     * @param address the address (http or https protocol included) to call the REST API at
     * @return a JsonReader object on this address
     */
    private static InputStream buildReader(String address)
    {
        try {
            URL url = new URL(address);
            HttpURLConnection con = (HttpURLConnection) url.openConnection();
            con.setRequestMethod("GET");
            con.setRequestProperty("Accept-Encoding", "gzip");
            con.connect();


            if("gzip".equals(con.getContentEncoding()))
                return new BufferedInputStream(new GZIPInputStream(con.getInputStream()));
            else
                return new BufferedInputStream(con.getInputStream());
        }
        catch(IOException e)
        {
            e.printStackTrace();
        }
        return null;
    }

    private static RawMeasurement buildMeasurement(DataInputStream stream)
    {
        try {
            RawMeasurement measurement = new RawMeasurement();
            measurement.setBarometer(stream.readShort());
            measurement.setDateStamp(LocalDateTime.ofEpochSecond(stream.readLong(), 0, ZoneOffset.UTC));
            measurement.setInsideTemp(stream.readShort());
            measurement.setInsideHum(stream.readShort());
            measurement.setOutsideTemp(stream.readShort());
            measurement.setOutsideHum(stream.readShort());
            measurement.setWindSpeed(stream.readShort());
            measurement.setAvgWindSpeed(stream.readShort());
            measurement.setWindDir(stream.readShort());
            measurement.setRainRate(stream.readShort());
            measurement.setUVLevel(stream.readShort());
            measurement.setSolarRad(stream.readShort());
            measurement.setXmitBatt(stream.readShort());
            measurement.setBattLevel(stream.readShort());
            measurement.setSunrise(stream.readShort());
            measurement.setSunset(stream.readShort());

            measurement.setStationId(stream.readShort() + "");
            return measurement;
        } catch(EOFException e) {
            return null;
        }
        catch (IOException e)
        {
            e.printStackTrace();
        }

        return null;
    }




    static class CacheStream extends InputStream
    {
        private InputStream is;
        FileOutputStream os;

        public CacheStream(InputStream is, String address)
        {
            this.is = is;

            address = DatabaseConnection.buildCacheFileName(address);
            try {
                if(!address.isEmpty())
                    os = new FileOutputStream(DatabaseConnection.getCacheDir() + address + ".bin");
            } catch (FileNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public int read() throws IOException {
            int ret = is.read();
            if(os != null)
                os.write(ret);
            return ret;
        }
    }


}



