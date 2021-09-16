package directory.events;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

import org.javatuples.Triplet;

import directory.Directory;
import directory.Utils;
import info.macias.sse.EventBroadcast;
import info.macias.sse.events.MessageEvent;
import wot.jtd.model.Thing;

public class EventSystem {

	private static List<Subscriber> subscriptions = new CopyOnWriteArrayList<>();
	private static List<Triplet<MessageEvent, MessageEvent, DirectoryEvent>> queue = new CopyOnWriteArrayList<>();
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
		}catch(Exception e) {
			Directory.LOGGER.error(e.toString());
		}
	}
	
	private void sendEventMessage(Subscriber subscriber, String lastEventId) {
		if(lastEventId!=null) {
			int index;
			if(lastEventId.equals("*")) {
				index = 0; 
			}else{
				for(index=0; index < queue.size(); index++) {
					boolean breakloop = queue.get(index).getValue0().getId().equals(lastEventId);
					if(breakloop) 
						break;
				}
			}
			sendPastEventMessages(subscriber, index);
		}
	}

	private void sendPastEventMessages(Subscriber subscriber, int skip) {
		queue.stream().skip(skip)
		 .filter(triplet -> subscriberInterested(subscriber,triplet.getValue2()))
		 .forEach(triplet -> sendEventMessage(subscriber, triplet.getValue0(), triplet.getValue1()));
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
    		MessageEvent extendedMesasage = new MessageEvent.Builder().setId(id).setEvent(event.getEvent()).setData(extendedData).build();

    		subscriptions.parallelStream()
    					 .filter(subscriber -> subscriberInterested(subscriber,event))
    					 .forEach(subscriber -> sendEventMessage(subscriber, mesasage, extendedMesasage));
    		Triplet<MessageEvent, MessageEvent, DirectoryEvent> pair = new Triplet<>(mesasage, extendedMesasage, event);
    		queue.add(pair);
    }

	private void sendEventMessage(Subscriber subscriber, MessageEvent mesasage, MessageEvent extendedMesasage) {
		try {
			if(subscriber.getDiff()) {
				subscriber.getClient().send(extendedMesasage);
			}else {
				subscriber.getClient().send(mesasage);
			}
		}catch(Exception e) {
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
	

	
	
}
