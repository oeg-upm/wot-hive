package directory;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;

import javax.jmdns.JmDNS;
import javax.jmdns.ServiceEvent;
import javax.jmdns.ServiceInfo;
import javax.jmdns.ServiceListener;


public class DNSN {

private static final String DNSN_NAME = "_directory._sub._wot";

	
	public static void initListeningDNSN() {
		try {
			// Create a JmDNS instance
			JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

			// Add a service listener
			jmdns.addServiceListener(DNSN_NAME, new WoTHiveListener());

			// Wait a bit
			Thread.sleep(30000);
		} catch (UnknownHostException e) {
			System.out.println(e.getMessage());
		} catch (IOException e) {
			System.out.println(e.getMessage());
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
	}

	private static class WoTHiveListener implements ServiceListener {
        @Override
        public void serviceAdded(ServiceEvent event) {
            System.out.println("Service added: " + event.getInfo());
        }

        @Override
        public void serviceRemoved(ServiceEvent event) {
            System.out.println("Service removed: " + event.getInfo());
        }

        @Override
        public void serviceResolved(ServiceEvent event) {
            System.out.println("Service resolved: " + event.getInfo());
        }
    }
	
	private static void registration()  {
	     try {
	            // Create a JmDNS instance
	            JmDNS jmdns = JmDNS.create(InetAddress.getLocalHost());

	            // Register a service
	            ServiceInfo serviceInfo = ServiceInfo.create("_http._tcp.local.", "wot", Directory.port, "path=index.html");
	            jmdns.registerService(serviceInfo);

	            // Wait a bit
	            Thread.sleep(25000);

	            // Unregister all services
	            jmdns.unregisterAllServices();

	        } catch (IOException e) {
	            System.out.println(e.getMessage());
	        } catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
	}
	
	
}
