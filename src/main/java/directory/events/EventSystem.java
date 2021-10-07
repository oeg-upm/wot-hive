package directory.events;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.stream.Stream;

import org.javatuples.Triplet;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import directory.Directory;
import directory.Utils;
import info.macias.sse.EventBroadcast;
import info.macias.sse.events.MessageEvent;
import wot.jtd.model.Thing;

public class EventSystem {

	private static List<Subscriber> subscriptions = new CopyOnWriteArrayList<>();
	public static final EventBroadcast broadcaster = new EventBroadcast();

	public EventSystem() {
		super();
	}

	// -- Subscription & update with past events

	public void addSubscriber(Subscriber newSubscriber, String lastEventId) {
		try {
			subscriptions.add(newSubscriber);
			broadcaster.addSubscriber(newSubscriber.getClient());
			sendEventMessage(newSubscriber, lastEventId);
		} catch (Exception e) {
			Directory.LOGGER.error(e.toString());
		}
	}

	private void sendEventMessage(Subscriber subscriber, String lastEventId) {
		if(lastEventId!=null) {
			boolean indexFound = lastEventId.equals("*");
			try (BufferedReader br = new BufferedReader(new FileReader(Directory.getConfiguration().getService().getEventsFile()))) {
				String line;
				while ((line = br.readLine()) != null) {
				    	Triplet<MessageEvent, MessageEvent, DirectoryEvent> triplet = transformRawEvent(line);
				    	indexFound |= triplet.getValue0().getId().equals(lastEventId);
				    	if(indexFound && subscriberInterested(subscriber, triplet.getValue2())) 
				    		sendEventMessage(subscriber, triplet.getValue0(), triplet.getValue1());
				}
			}catch(Exception e) {
				e.printStackTrace();
			}
		}
			
		
	}



	private Triplet<MessageEvent, MessageEvent, DirectoryEvent> transformRawEvent(String rawLine) {
		JsonObject json = new Gson().fromJson(rawLine, JsonObject.class);
		String id = json.get("id").getAsString();
		String simple = json.get("simple").getAsString();
		String extended = json.get("extended").getAsString();
		String type = json.get("type").getAsString();
		MessageEvent mesasage = new MessageEvent.Builder().setId(id).setEvent(type).setData(simple).build();
		MessageEvent extendedMesasage = new MessageEvent.Builder().setId(id).setEvent(type).setData(extended).build();
		Triplet<MessageEvent, MessageEvent, DirectoryEvent> triplet = new Triplet<>(mesasage, extendedMesasage,
				DirectoryEvent.valueOf(type));
		return triplet;
	}

	// -- Sending new events

	public void igniteEvent(String thingId, DirectoryEvent event) {
		igniteEvent(thingId, event, null);
	}

	public void igniteEvent(String thingId, DirectoryEvent event, Thing thing) {
		String id = Utils.buildMessage(thingId, "/events/", event.getEvent());
		String data = Utils.buildMessage("{\"id\": \"", thingId, "\"}");
		String extendedData = prepareExtendedMessage(thing);
		MessageEvent mesasage = new MessageEvent.Builder().setId(id).setEvent(event.getEvent()).setData(data).build();
		MessageEvent extendedMesasage = new MessageEvent.Builder().setId(id).setEvent(event.getEvent())
				.setData(extendedData).build();

		subscriptions.parallelStream().filter(subscriber -> subscriberInterested(subscriber, event))
				.forEach(subscriber -> sendEventMessage(subscriber, mesasage, extendedMesasage));
		Triplet<MessageEvent, MessageEvent, DirectoryEvent> triplet = new Triplet<>(mesasage, extendedMesasage, event);
		storeEvent(triplet);
	}

	private void sendEventMessage(Subscriber subscriber, MessageEvent mesasage, MessageEvent extendedMesasage) {
		try {
			if (subscriber.getDiff()) {
				subscriber.getClient().send(extendedMesasage);
			} else {
				subscriber.getClient().send(mesasage);
			}
		} catch (Exception e) {
			Directory.LOGGER.error(e.toString());
		}
	}

	private boolean subscriberInterested(Subscriber subscriber, DirectoryEvent event) {
		return subscriber.getEventType().equals(DirectoryEvent.ALL) || subscriber.getEventType().equals(event);
	}

	private String prepareExtendedMessage(Thing thing) {
		String extendedMsg = null;
		try {
			if (thing != null)
				extendedMsg = thing.toJson().toString();
		} catch (Exception e) {
			Directory.LOGGER.error(e.toString());
		}
		return extendedMsg;
	}

	// storing events

	private void storeEvent(Triplet<MessageEvent, MessageEvent, DirectoryEvent> triplet) {

		JsonObject json = new JsonObject();
		json.addProperty("id", triplet.getValue0().getId());
		json.addProperty("simple", triplet.getValue0().getData());
		json.addProperty("extended", triplet.getValue1().getData());
		json.addProperty("type", triplet.getValue2().toString());
		String eventsFile = Directory.getConfiguration().getService().getEventsFile();
		try (FileWriter fw = new FileWriter(eventsFile, true)) {
			fw.write(json.toString() + "\n");
			while (numberOfEvents(eventsFile) >= Directory.getConfiguration().getService().getEventsSize())
				removeFirstLine(eventsFile);
		} catch (Exception e) {
			e.printStackTrace();
		}

	}

	private int numberOfEvents(String eventsFile) throws IOException {
		int count = 0;
		try (FileInputStream stream = new FileInputStream(eventsFile)) {
			byte[] buffer = new byte[8192];
			int n;
			while ((n = stream.read(buffer)) > 0) {
				for (int i = 0; i < n; i++) {
					if (buffer[i] == '\n')
						count++;
				}
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
		return count;

	}

	public static void removeFirstLine(String fileName) throws IOException {
		try (RandomAccessFile raf = new RandomAccessFile(fileName, "rw")) {
			// Initial write position
			long writePosition = raf.getFilePointer();
			raf.readLine();
			// Shift the next lines upwards.
			long readPosition = raf.getFilePointer();

			byte[] buff = new byte[1024];
			int n;
			while (-1 != (n = raf.read(buff))) {
				raf.seek(writePosition);
				raf.write(buff, 0, n);
				readPosition += n;
				writePosition += n;
				raf.seek(readPosition);
			}
			raf.setLength(writePosition);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
