// FIR_IDENTICAL
// FILE: Vehicle.java

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class Vehicle {
    private String make;
    private String model;
}

// FILE: Car.java

import lombok.Getter;
import lombok.experimental.SuperBuilder;

@Getter
@SuperBuilder
public class Car extends Vehicle {
    private int numberOfDoors;
}

// FILE: test.kt
fun test() {
    val carSuperBuilder: Car.CarBuilder<*, *>? = Car.builder()
}

// FILE: lombok.config
lombok.builder.className=Special*SuperBuilder
