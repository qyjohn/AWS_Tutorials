import java.util.*;
import com.amazonaws.*;
import com.amazonaws.auth.*;
import com.amazonaws.auth.profile.*;
import com.amazonaws.regions.*;
import com.amazonaws.services.ec2.*;
import com.amazonaws.services.ec2.model.*;

public class TestEC2 
{
        public AmazonEC2Client client;

        public TestEC2()
        {
                client = new AmazonEC2Client();
                client.configureRegion(Regions.AP_SOUTHEAST_2);
        }

        public void listInstances()
        {
                try 
                {
                        DescribeInstancesResult result = client.describeInstances();
                        List<Reservation> reservations = result.getReservations();
                        for (Reservation reservation: reservations)
                        {
                                String reservation_id = reservation.getReservationId();
                                System.out.println("Reservation: " + reservation_id);
                                List<Instance> instances = reservation.getInstances();
                                for (Instance instance: instances)
                                {
                                        String id = instance.getInstanceId();
                                        String state = instance.getState().getName();
                                        System.out.println("\t" + id + "\t" + state);
                                }
                        }
                } catch (Exception e) 
                {
                        System.out.println(e.getMessage());
                        e.printStackTrace();
                }
        }

        public static void main(String[] args)
        {
                TestEC2 test = new TestEC2();
                test.listInstances();
        }
}
