import org.junit.jupiter.api.*;
import java.util.*;
import java.io.*;

import static org.junit.jupiter.api.Assertions.*;

public class CarParkingTest {

    private CarParking carParking;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();
    private final PrintStream originalOut = System.out;

    @BeforeEach
    void setUp() {
        carParking = new CarParking();
        carParking.clear(); // Ensure clean state
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    void tearDown() {
        System.setOut(originalOut);
        carParking.clear();
        outContent.reset();
    }

    // ========== Arrival Tests ==========

    @Test
    @DisplayName("Test car arrival when garage is empty")
    void testArrivalToEmptyGarage() {
        Car car = new Car("UB11-11");
        carParking.arrival(car);

        assertEquals(1, CarParking.parking.size());
        assertEquals("UB11-11", CarParking.parking.peek().plate);
        assertTrue(outContent.toString().contains("There is room."));
    }

    @Test
    @DisplayName("Test multiple car arrivals")
    void testMultipleArrivals() {
        Car car1 = new Car("AA11-11");
        Car car2 = new Car("BB22-22");
        Car car3 = new Car("CC33-33");

        carParking.arrival(car1);
        carParking.arrival(car2);
        carParking.arrival(car3);

        assertEquals(3, CarParking.parking.size());
        assertEquals("CC33-33", CarParking.parking.peek().plate); // Last in, on top
    }

    @Test
    @DisplayName("Test car arrival when garage is full")
    void testArrivalToFullGarage() {
        // Fill garage to capacity
        for (int i = 0; i < CarParking.MAX_SIZE; i++) {
            carParking.arrival(new Car("CAR" + i));
        }

        outContent.reset(); // Clear previous output
        Car extraCar = new Car("OVER");
        carParking.arrival(extraCar);

        assertEquals(CarParking.MAX_SIZE, CarParking.parking.size());
        assertFalse(CarParking.parking.stream()
                .anyMatch(c -> c.plate.equals("OVER")));
        assertTrue(outContent.toString().contains("Garage full"));
    }

    @Test
    @DisplayName("Test arrival at exactly MAX_SIZE boundary")
    void testArrivalAtBoundary() {
        for (int i = 0; i < CarParking.MAX_SIZE - 1; i++) {
            carParking.arrival(new Car("CAR" + i));
        }

        Car lastCar = new Car("LAST");
        carParking.arrival(lastCar);

        assertEquals(CarParking.MAX_SIZE, CarParking.parking.size());
        assertEquals("LAST", CarParking.parking.peek().plate);
    }

    // ========== Departure Tests ==========

    @Test
    @DisplayName("Test departure from empty garage")
    void testDepartureFromEmptyGarage() {
        carParking.departure(new Car("XX99-99"));

        assertEquals(0, CarParking.parking.size());
        assertTrue(outContent.toString().contains("Garage is empty."));
    }

    @Test
    @DisplayName("Test departure of car at top of stack")
    void testDepartureTopCar() {
        carParking.arrival(new Car("AA11-11"));
        carParking.arrival(new Car("BB22-22"));
        carParking.arrival(new Car("CC33-33"));

        outContent.reset();
        carParking.departure(new Car("CC33-33"));

        assertEquals(2, CarParking.parking.size());
        assertEquals("BB22-22", CarParking.parking.peek().plate);
        assertTrue(outContent.toString().contains("0 cars moved out."));
    }

    @Test
    @DisplayName("Test departure of car in middle of stack")
    void testDepartureMiddleCar() {
        carParking.arrival(new Car("AA11-11"));
        carParking.arrival(new Car("BB22-22"));
        carParking.arrival(new Car("CC33-33"));

        outContent.reset();
        carParking.departure(new Car("BB22-22"));

        assertEquals(2, CarParking.parking.size());
        assertTrue(outContent.toString().contains("1 cars moved out."));
        
        List<String> plates = new ArrayList<>();
        for (Car c : CarParking.parking) {
            plates.add(c.plate);
        }
        assertTrue(plates.contains("AA11-11"));
        assertTrue(plates.contains("CC33-33"));
        assertFalse(plates.contains("BB22-22"));
    }

    @Test
    @DisplayName("Test departure of car at bottom of stack")
    void testDepartureBottomCar() {
        carParking.arrival(new Car("AA11-11"));
        carParking.arrival(new Car("BB22-22"));
        carParking.arrival(new Car("CC33-33"));

        outContent.reset();
        carParking.departure(new Car("AA11-11"));

        assertEquals(2, CarParking.parking.size());
        assertTrue(outContent.toString().contains("2 cars moved out."));
    }

    @Test
    @DisplayName("Test departure when car not in garage")
    void testDepartureCarNotFound() {
        carParking.arrival(new Car("UB12-12"));
        carParking.arrival(new Car("AA11-11"));

        outContent.reset();
        carParking.departure(new Car("XX99-99"));

        assertEquals(2, CarParking.parking.size());
        assertTrue(outContent.toString().contains("This car not in the garage."));
    }

    @Test
    @DisplayName("Test stack order is preserved after unsuccessful departure")
    void testOrderPreservedAfterFailedDeparture() {
        carParking.arrival(new Car("FIRST"));
        carParking.arrival(new Car("SECOND"));
        carParking.arrival(new Car("THIRD"));

        carParking.departure(new Car("NOTHERE"));

        assertEquals("THIRD", CarParking.parking.pop().plate);
        assertEquals("SECOND", CarParking.parking.pop().plate);
        assertEquals("FIRST", CarParking.parking.pop().plate);
    }

    // ========== Output Tests ==========

    @Test
    @DisplayName("Test output when garage is empty")
    void testOutputEmpty() {
        carParking.output();

        String output = outContent.toString();
        assertTrue(output.contains("Cars currently in garage:"));
        assertTrue(output.contains("None."));
    }

    @Test
    @DisplayName("Test output with cars in garage")
    void testOutputWithCars() {
        carParking.arrival(new Car("UB11-11"));
        carParking.arrival(new Car("AA22-22"));

        outContent.reset();
        carParking.output();

        String output = outContent.toString();
        assertTrue(output.contains("Cars currently in garage:"));
        assertTrue(output.contains("UB11-11"));
        assertTrue(output.contains("AA22-22"));
    }

    // ========== Input Tests ==========

    @Test
    @DisplayName("Test input file reading")
    void testInputFileReading() throws IOException {
        // Create temporary test file
        File tempFile = File.createTempFile("test_cars", ".txt");
        tempFile.deleteOnExit();

        try (PrintWriter writer = new PrintWriter(tempFile)) {
            writer.println("A UB11-11");
            writer.println("A BB22-22");
            writer.println("D UB11-11");
        }

        List<String[]> commands = carParking.input(tempFile.getAbsolutePath());

        assertEquals(3, commands.size());
        assertArrayEquals(new String[]{"A", "UB11-11"}, commands.get(0));
        assertArrayEquals(new String[]{"A", "BB22-22"}, commands.get(1));
        assertArrayEquals(new String[]{"D", "UB11-11"}, commands.get(2));
    }

    @Test
    @DisplayName("Test input with non-existent file")
    void testInputNonExistentFile() {
        List<String[]> commands = carParking.input("nonexistent.txt");
        assertNotNull(commands);
        assertTrue(commands.isEmpty());
    }

    // ========== Process Tests ==========

    @Test
    @DisplayName("Test process method with mixed commands")
    void testProcessCommands() {
        List<String[]> commands = Arrays.asList(
            new String[]{"A", "CAR1"},
            new String[]{"A", "CAR2"},
            new String[]{"D", "CAR1"},
            new String[]{"A", "CAR3"}
        );

        carParking.process(commands);

        assertEquals(2, CarParking.parking.size());
        List<String> plates = new ArrayList<>();
        for (Car c : CarParking.parking) {
            plates.add(c.plate);
        }
        assertTrue(plates.contains("CAR2"));
        assertTrue(plates.contains("CAR3"));
        assertFalse(plates.contains("CAR1"));
    }

    @Test
    @DisplayName("Test process with case-insensitive commands")
    void testProcessCaseInsensitive() {
        List<String[]> commands = Arrays.asList(
            new String[]{"a", "CAR1"},
            new String[]{"A", "CAR2"},
            new String[]{"d", "CAR1"}
        );

        carParking.process(commands);

        assertEquals(1, CarParking.parking.size());
        assertEquals("CAR2", CarParking.parking.peek().plate);
    }

    // ========== Edge Cases ==========

    @Test
    @DisplayName("Test clearing garage")
    void testClearGarage() {
        carParking.arrival(new Car("CAR1"));
        carParking.arrival(new Car("CAR2"));

        carParking.clear();

        assertEquals(0, CarParking.parking.size());
        assertEquals(0, carParking.temp.size());
    }

    @Test
    @DisplayName("Test temp stack is empty after departure")
    void testTempStackEmptyAfterDeparture() {
        carParking.arrival(new Car("CAR1"));
        carParking.arrival(new Car("CAR2"));
        carParking.arrival(new Car("CAR3"));

        carParking.departure(new Car("CAR1"));

        assertTrue(carParking.temp.isEmpty());
    }

    @Test
    @DisplayName("Test departure and immediate arrival")
    void testDepartureAndArrival() {
        for (int i = 0; i < CarParking.MAX_SIZE; i++) {
            carParking.arrival(new Car("CAR" + i));
        }

        carParking.departure(new Car("CAR5"));
        Car newCar = new Car("NEWCAR");
        carParking.arrival(newCar);

        assertEquals(CarParking.MAX_SIZE, CarParking.parking.size());
        assertTrue(CarParking.parking.stream()
                .anyMatch(c -> c.plate.equals("NEWCAR")));
    }

    @Test
    @DisplayName("Test duplicate car plates")
    void testDuplicateCarPlates() {
        carParking.arrival(new Car("DUP"));
        carParking.arrival(new Car("DUP"));
        carParking.arrival(new Car("OTHER"));

        // Both duplicates should be added
        assertEquals(3, CarParking.parking.size());

        // Departure should only remove one
        carParking.departure(new Car("DUP"));
        assertEquals(2, CarParking.parking.size());
    }
}