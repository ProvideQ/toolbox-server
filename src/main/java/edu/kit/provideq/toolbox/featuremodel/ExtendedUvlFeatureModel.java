package edu.kit.provideq.toolbox.featuremodel;

import java.util.List;

public class ExtendedUvlFeatureModel {
  public static List<String> getExamples() {
    return List.of("""
            namespace Sandwich
                            
            features
                Sandwich {extended__}   \s
                    mandatory
                        Bread   \s
                            alternative
                                "Full Grain" {Calories 203, Price 1.99, Organic true}
                                Flatbread {Calories 90, Price 0.79, Organic true}
                                Toast {Calories 250, Price 0.99, Organic false}
                    optional
                        Cheese   \s
                            optional
                                Gouda   \s
                                    alternative
                                        Sprinkled {Fat {value 35, unit "g"}}
                                        Slice {Fat {value 35, unit "g"}}
                                Cheddar
                                "Cream Cheese"
                        Meat   \s
                            or
                                "Salami" {Producer "Farmer Bob"}
                                Ham {Producer "Farmer Sam"}
                                "Chicken Breast" {Producer "Farmer Sam"}
                        Vegetables   \s
                            optional
                                "Cucumber"
                                Tomatoes
                                Lettuce
            """);
  }
}
