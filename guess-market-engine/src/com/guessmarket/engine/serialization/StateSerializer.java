package com.guessmarket.engine.serialization;

import com.guessmarket.engine.exception.MarketException;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StreamCorruptedException;

/** Binary save/load of the whole {@link MarketSnapshot}. */
public final class StateSerializer {

    private static final Logger LOG = LogManager.getLogger(StateSerializer.class);

    private StateSerializer() {}

    public static void save(MarketSnapshot snapshot, String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Save file path cannot be empty.");
        }
        File file = new File(filePath.trim());
        try (ObjectOutputStream out = new ObjectOutputStream(new FileOutputStream(file))) {
            out.writeObject(snapshot);
            LOG.info("Wrote market state ({} event(s), {} user(s)) to {} [{} bytes]",
                    snapshot.events().size(), snapshot.users().size(), file, file.length());
        } catch (IOException e) {
            throw new MarketException("Failed to save state to [" + filePath + "]: " + e.getMessage(), e);
        }
    }

    public static MarketSnapshot load(String filePath) throws MarketException {
        if (filePath == null || filePath.isBlank()) {
            throw new MarketException("Load file path cannot be empty.");
        }
        File file = new File(filePath.trim());
        if (!file.exists()) {
            throw new MarketException("State file does not exist at path: " + filePath);
        }
        try (ObjectInputStream in = new ObjectInputStream(new FileInputStream(file))) {
            MarketSnapshot snapshot = (MarketSnapshot) in.readObject();
            LOG.info("Read market state ({} event(s), {} user(s)) from {}",
                    snapshot.events().size(), snapshot.users().size(), file);
            return snapshot;
        } catch (StreamCorruptedException | ClassCastException e) {
            throw new MarketException(
                    "That file is not a valid Guess Market saved state (did you pick an XML file by mistake?).");
        } catch (ClassNotFoundException | InvalidClassException e) {
            throw new MarketException("The state file was written by an incompatible version of the system.");
        } catch (IOException e) {
            throw new MarketException("Failed to load saved state from [" + filePath + "]: " + e.getMessage(), e);
        }
    }
}
