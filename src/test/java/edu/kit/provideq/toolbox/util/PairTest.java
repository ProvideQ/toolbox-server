package edu.kit.provideq.toolbox.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;

import org.junit.jupiter.api.Test;

class PairTest {

  @Test
  void testPairConstructorAndAccessors() {
    Pair<String, Integer> pair = new Pair<>("test", 42);

    assertEquals("test", pair.first());
    assertEquals(42, pair.second());
  }

  @Test
  void testCopyWithFirst() {
    Pair<String, Integer> original = new Pair<>("original", 10);
    Pair<String, Integer> updated = original.copyWithFirst("updated");

    assertEquals("updated", updated.first());
    assertEquals(10, updated.second());
    assertEquals("original", original.first());
    assertNotSame(original, updated);
  }

  @Test
  void testCopyWithSecond() {
    Pair<String, Integer> original = new Pair<>("test", 10);
    Pair<String, Integer> updated = original.copyWithSecond(20);

    assertEquals("test", updated.first());
    assertEquals(20, updated.second());
    assertEquals(10, original.second());
    assertNotSame(original, updated);
  }

  @Test
  void testPairWithDifferentTypes() {
    Pair<Double, String> pair = new Pair<>(3.14, "pi");

    assertEquals(3.14, pair.first());
    assertEquals("pi", pair.second());

    Pair<Double, String> updatedFirst = pair.copyWithFirst(2.71);
    assertEquals(2.71, updatedFirst.first());
    assertEquals("pi", updatedFirst.second());

    Pair<Double, String> updatedSecond = pair.copyWithSecond("e");
    assertEquals(3.14, updatedSecond.first());
    assertEquals("e", updatedSecond.second());
  }
}
