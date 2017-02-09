package com.net2plan;

import java.util.ArrayList;

/**
 * @author Jorge San Emeterio
 * @date 09-Feb-17
 */
public class Testing
{
    public static void main(String[] args)
    {
        final ArrayList<String> objects = new ArrayList<>();

        objects.add("Hola");
        objects.add("Mundo");
        objects.add("Adios");

        objects.subList(1, objects.size()).clear();

        for (String object : objects)
        {
            System.out.println(object);
        }
    }
}
