import java.util.List;

public class Main {
    public static void main(String[] args) {
        CarParking carParking = new CarParking();
        List<String[]> commands = carParking.input("src/cars.txt");
        carParking.process(commands);
        carParking.output();
    }
}
