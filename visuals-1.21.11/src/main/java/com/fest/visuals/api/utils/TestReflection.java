package com.fest.visuals.api.utils;
import net.minecraft.client.input.CharacterEvent;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import java.lang.reflect.Method;

public class TestReflection {
    public static void print() {
        System.out.println("CharacterEvent methods:");
        for (Method m : CharacterEvent.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
        System.out.println("KeyEvent methods:");
        for (Method m : KeyEvent.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
        System.out.println("MouseButtonEvent methods:");
        for (Method m : MouseButtonEvent.class.getDeclaredMethods()) {
            System.out.println(m.getName());
        }
    }
}