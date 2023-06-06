package edu.kit.provideq.toolbox.convert;

import edu.kit.provideq.toolbox.exception.ConversionException;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest
public class UvlToDimacsCNFTests {
    @Test
    public void TestSimple() throws ConversionException {
        String converted = UvlToDimacsCNF.convert("""   
                features
                	Sandwich {extended__}
                		mandatory
                			Bread
                				alternative
                					"Full Grain" {Calories 203, Price 1.99, Organic true}
                					Flatbread {Calories 90, Price 0.79, Organic true}
                					Toast {Calories 250, Price 0.99, Organic false}
                		optional
                			Cheese
                				optional
                					Gouda
                						alternative
                							Sprinkled {Fat {value 35, unit "g"}}
                							Slice {Fat {value 35, unit "g"}}
                					Cheddar
                					"Cream Cheese"
                			Meat
                				or
                					"Salami" {Producer "Farmer Bob"}
                					Ham {Producer "Farmer Sam"}
                					"Chicken Breast" {Producer "Farmer Sam"}
                			Vegetables
                				optional
                					"Cucumber"
                					Tomatoes
                					Lettuce
                """);

        assertEquals(converted.replace("\r\n", "\n"), ("""
                c 1 Sandwich
                c 2 Bread
                c 3 Full Grain
                c 4 Flatbread
                c 5 Toast
                c 6 Cheese
                c 7 Gouda
                c 8 Sprinkled
                c 9 Slice
                c 10 Cheddar
                c 11 Cream Cheese
                c 12 Meat
                c 13 Salami
                c 14 Ham
                c 15 Chicken Breast
                c 16 Vegetables
                c 17 Cucumber
                c 18 Tomatoes
                c 19 Lettuce
                p cnf 19 27
                1 0
                1 -2 0
                1 -6 0
                1 -12 0
                1 -16 0
                2 -1 0
                2 -3 0
                2 -4 0
                2 -5 0
                3 4 5 -2 0
                -3 -4 0
                -3 -5 0
                -4 -5 0
                6 -7 0
                6 -10 0
                6 -11 0
                7 -8 0
                7 -9 0
                8 9 -7 0
                -8 -9 0
                12 -13 0
                12 -14 0
                12 -15 0
                13 14 15 -12 0
                16 -17 0
                16 -18 0
                16 -19 0
                """)
        );
    }
}
