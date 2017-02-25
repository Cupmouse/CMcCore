package net.cupmouse.minecraft.test;

import net.cupmouse.minecraft.Utilities;

import javax.xml.bind.DatatypeConverter;
import java.util.UUID;

public class ConvertTest {

    public static void main(String[] args) {
        for (int j = 0; j < 10; j++) {
            int numbers = 1_000_000;

            UUID[] uuids = new UUID[numbers];
            for (int i = 0; i < numbers; i++) {
                uuids[i] = UUID.randomUUID();
            }

            System.out.println("convertion starts!");

            long startTime = System.nanoTime();
            for (int i = 0; i < numbers; i++) {
                Utilities.convertUUIDtoBytes(uuids[i]);
            }

            System.out.println(System.nanoTime() - startTime);
        }

        UUID uuid = UUID.randomUUID();
        byte[] bytes = Utilities.convertUUIDtoBytes(uuid);

        System.out.println(uuid + " : " + DatatypeConverter.printHexBinary(bytes));
    }
}
