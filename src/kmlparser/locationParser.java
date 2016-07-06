/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package kmlparser;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public class locationParser {
    public static void main (String[] args) {
        ArrayList<LatLngTime> parsed = parse("history-2016-07-05.kml");
        ArrayList<LatLngTime> result = getUserstay(parsed);
        System.out.println("result size: "+result.size());
        for(LatLngTime llt: result)
            System.out.println(llt.toString());
    }
    public static ArrayList<LatLngTime> parse(String fileName) {
        DocumentBuilderFactory factory;
        DocumentBuilder builder;
        factory = DocumentBuilderFactory.newInstance();
        ArrayList<LatLngTime> result = new ArrayList<>();
        
        try {
            builder = factory.newDocumentBuilder();
            Document document = builder.parse(new File(fileName));
            List<String> name= new ArrayList<>();
            NodeList nodeList = document.getDocumentElement().getChildNodes();
         
            //get gx:Track layer
            Node node = nodeList.item(0);
           
            if(node.getNodeType() == Node.ELEMENT_NODE) {
                Element elem = (Element) node;
                NodeList whenList = elem.getElementsByTagName("when");
                NodeList locationList = elem.getElementsByTagName("gx:coord");
                
                String format = "yyyy-MM-dd'T'HH:mm:ss.SSS";
                
                for(int i = 0; i < whenList.getLength(); i++) {
                    LatLngTime llt = new LatLngTime ();
                    Node whenNode = whenList.item(i);
                    if(whenNode.getNodeType() == Node.ELEMENT_NODE) {
                        String time = whenNode.getChildNodes().item(0).getNodeValue();
                        SimpleDateFormat formatter = new SimpleDateFormat(format);
                        Date date = formatter.parse(time);
                        long millis = date.getTime();
                        llt.setStartTime(millis);
                        llt.setEndTime(millis);
                        //System.out.println(time + ": " +millis);
                    }

                    Node locNode = locationList.item(i);
                    if(locNode.getNodeType() == Node.ELEMENT_NODE) {
                        String coordinate = locNode.getChildNodes().item(0).getNodeValue();
                        String[] split = coordinate.split(" ");
                        double lng = Double.valueOf(split[0]);
                        double lat = Double.valueOf(split[1]);
                        llt.setLat(lat);
                        llt.setLng(lng);
                        //System.out.println(split[0] + " " + lng + " "+ split[1] +" "+ lat);
                    }
                    result.add(llt);
                }
            }
        }
        
        catch (ParserConfigurationException e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        catch (IOException e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        catch (SAXException e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        catch (Exception e) {
            System.err.println( e.getClass().getName() + ": " + e.getMessage() );
            System.exit(0);
        }
        
        return result;
    }
    
     public static ArrayList<LatLngTime> getUserstay(ArrayList<LatLngTime> inputList) {
        ArrayList<LatLngTime> outputList = new ArrayList<>();
        LatLngTime stay = null;
        if(!inputList.isEmpty()) {
            LatLngTime temp = inputList.get(0);
            stay = new LatLngTime(temp.getLat(), temp.getLng(), temp.getStartTime(), temp.getEndTime());
        }

        for(int i = 1; i < inputList.size(); i++) {
            LatLngTime temp = inputList.get(i);
            double tempLat = temp.getLat();
            double tempLng = temp.getLng();
            
            if(distance(tempLat, tempLng, stay.getLat(), stay.getLng()) <= 100) {
                stay.setEndTime(temp.getEndTime());
            }
            else {
                //1200000 milliseconds == 20 mins
                if(stay.getEndTime() - stay.getStartTime() >= 600000){ 
                    outputList.add(stay);    
                }
                //this if-block handles special case of a sudden appeared coordinates
                //In Google Timeline, it usually means the end of a userstay
                if(temp.getStartTime() - stay.getEndTime() >= 1200000) {
                    //the new userstay will start 10 mins after the previous one(assume 10-mins moving)
                    long startTime = stay.getEndTime()+600000;
                    stay = new LatLngTime(temp.getLat(), temp.getLng(),startTime, temp.getEndTime());
                    outputList.add(stay);
                    if(i < inputList.size()-1) {
                        stay = new LatLngTime(inputList.get(i+1).getLat(), inputList.get(i+1).getLng(),
                                inputList.get(i+1).getStartTime(), inputList.get(i+1).getEndTime());
                        i++;
                    }
                    continue;
                }
         
                stay = new LatLngTime(temp.getLat(), temp.getLng(),temp.getStartTime(), temp.getEndTime());
            }
        }
        if(stay.getEndTime() - stay.getStartTime() >= 600000){ 
                    outputList.add(stay);    
        }
        //System.out.println("output: "+outputList.size()+" input: "+inputList.size());
        if(outputList.size() == inputList.size())
            return outputList;
        else 
            return getUserstay(outputList);
       
    }
     
      /*
     * Calculate distance between two points in latitude and longitude
     * Uses Haversine method as its base.
     * lat1, lon1 Start point lat2, lon2 End point
     * @returns distance in Meters
    */
    private static double distance(double lat1, double lon1, double lat2,
                                  double lon2) {

        final int R = 6371; // Radius of the earth

        Double latDistance = Math.toRadians(lat2 - lat1);
        Double lonDistance = Math.toRadians(lon2 - lon1);
        Double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        Double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        double distance = R * c * 1000; // convert to meters


        distance = Math.pow(distance, 2);

        return Math.sqrt(distance);
    }
}
