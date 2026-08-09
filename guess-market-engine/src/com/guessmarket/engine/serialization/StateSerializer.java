package com.guessmarket.engine.serialization;

import com.guessmarket.engine.exception.MarketException;
import com.guessmarket.engine.model.Event;

import java.io.*;
import java.util.List;
import java.util.Map;

public class StateSerializer {

    public static void saveEngineState(Map<Integer, Event> events, String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Save file path cannot be empty.");
        }

        File file = new File(filePath.trim());
        try (ObjectOutputStream oos = new ObjectOutputStream(new FileOutputStream(file))) {
            oos.writeObject(events);
        } catch (IOException e) {
            throw new MarketException("Failed to save state to file [" + filePath + "]: " + e.getMessage(), e);
        }
    }

    @SuppressWarnings("unchecked")
    public static Map<Integer, Event> loadEngineState(String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Load file path cannot be empty.");
        }

        File file = new File(filePath.trim());
        if (!file.exists()) {
            throw new MarketException("State file does not exist at path: " + filePath);
        }

        try (ObjectInputStream ois = new ObjectInputStream(new FileInputStream(file))) {
            return (Map<Integer, Event>) ois.readObject();
        } catch (StreamCorruptedException e) {
            throw new MarketException(
                    "The selected file exists, but it is not a valid Guess Market saved state file " +
                            "(e.g., you may have selected an XML file instead)."
            );
        } catch (ClassNotFoundException | java.io.InvalidClassException e) {
            throw new MarketException(
                    "The state file format is incompatible with the current version of the system."
            );
        } catch (IOException e) {
            throw new MarketException("Failed to load saved state from file [" + filePath + "]: " + e.getMessage(), e);
        }
    }
}