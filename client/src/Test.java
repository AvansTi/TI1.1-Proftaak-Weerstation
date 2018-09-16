import java.time.LocalDateTime;
import java.util.ArrayList;

public class Test {
    public static void main(String[] args)
    {
        System.out.println(DatabaseConnection.getMostRecentMeasurement());
        for(int i = 1; i < 30; i++) {
            long time = System.nanoTime();
            ArrayList<RawMeasurement> measurements = DatabaseConnection.getMeasurementsLastMonths(i);
            System.out.println(i + " months, " + measurements.size() + " measurements, " + (System.nanoTime() - time) / 1e9 + " seconds");
        }
    }


}
