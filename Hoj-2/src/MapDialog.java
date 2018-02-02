// Kartankatseluohjelman graafinen k�ytt�liittym�

import javax.swing.*;
import javax.swing.event.*;
import java.awt.*;
import java.awt.event.*;
import java.net.*;
import java.lang.StringBuilder;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.apache.commons.io.FileUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
 
public class MapDialog extends JFrame {
 
  // Käyttöliittymän komponentit
 
  private JLabel imageLabel = new JLabel();
  private JPanel leftPanel = new JPanel();
 
  private JButton refreshB = new JButton("Päivitä");
  private JButton leftB = new JButton("<");
  private JButton rightB = new JButton(">");
  private JButton upB = new JButton("^");
  private JButton downB = new JButton("v");
  private JButton zoomInB = new JButton("+");
  private JButton zoomOutB = new JButton("-");
  private StringBuilder getMapUrl = new StringBuilder("http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetMap&BBOX=-180.00,-090.00,+180.00,+090.00&SRS=EPSG:4326&WIDTH=953&HEIGHT=480&LAYERS=country_bounds,continents,cities&STYLES=&FORMAT=image/png&TRANSPARENT=true");
  
  double xMin=-180.0, xMax=180.0, yMin=-90.0, yMax=90.0, kerroin=10;
  
  public MapDialog() throws Exception {
	  

 
    // Valmistele ikkuna ja lisää siihen komponentit
 
    setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
    getContentPane().setLayout(new BorderLayout());
 
    String url = getMapUrl.toString();
    imageLabel.setIcon(new ImageIcon(new URL(url)));
 
    add(imageLabel, BorderLayout.EAST);
 
    ButtonListener bl = new ButtonListener();
    refreshB.addActionListener(bl);  
    leftB.addActionListener(bl);
    rightB.addActionListener(bl);
    upB.addActionListener(bl);
    downB.addActionListener(bl);
    zoomInB.addActionListener(bl);
    zoomOutB.addActionListener(bl);
 
    leftPanel.setLayout(new BoxLayout(leftPanel, BoxLayout.Y_AXIS));
    leftPanel.setBorder(BorderFactory.createEmptyBorder(2, 2, 2, 2));
    leftPanel.setMaximumSize(new Dimension(100, 600));
 
    //GetCababilties ja sen mukaisen ikkunan asetusten luonti
    
    try {
		URL srcUrl = new URL("http://demo.mapserver.org/cgi-bin/wms?SERVICE=WMS&VERSION=1.1.1&REQUEST=GetCapabilities");
		try {

		File fXmlFile = new File("xmlTiedosto");
		FileUtils.copyURLToFile(srcUrl, fXmlFile);
		
		DocumentBuilderFactory dbFactory = DocumentBuilderFactory.newInstance();
		DocumentBuilder dBuilder = dbFactory.newDocumentBuilder();
		Document doc = dBuilder.parse(fXmlFile);

		doc.getDocumentElement().normalize();


		NodeList nList = doc.getElementsByTagName("Layer");

		for (int temp = 0; temp < nList.getLength(); temp++) {

			Node nNode = nList.item(temp);


			if (nNode.getNodeType() == Node.ELEMENT_NODE) {

				Element eElement = (Element) nNode;
				if (temp == 0) continue;
				leftPanel.add(new LayerCheckBox(eElement.getElementsByTagName("Name").item(0).getTextContent(), eElement.getElementsByTagName("Title").item(0).getTextContent(), false));


			}
		}
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	} catch (MalformedURLException e) {
		
	}
 
    leftPanel.add(refreshB);
    leftPanel.add(Box.createVerticalStrut(20));
    leftPanel.add(leftB);
    leftPanel.add(rightB);
    leftPanel.add(upB);
    leftPanel.add(downB);
    leftPanel.add(zoomInB);
    leftPanel.add(zoomOutB);
 
    add(leftPanel, BorderLayout.WEST);
 
    pack();
    setVisible(true);
 
  }
 
  public static void main(String[] args) throws Exception {
    new MapDialog();
  }
 
  // Kontrollinappien kuuntelija
  private class ButtonListener implements ActionListener{
    public void actionPerformed(ActionEvent e) {
      if(e.getSource() == refreshB) {
    	  try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
      }
      
      if(e.getSource() == leftB) {
        // VASEMMALLE SIIRTYMINEN KARTALLA
    	coordinateChange(2);
    	try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
    	  
      }
      if(e.getSource() == rightB) {
        // OIKEALLE SIIRTYMINEN KARTALLA
    	  System.out.println("funkkariin");
    	  
    	  coordinateChange(3);
    	  
    	  try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
    	  
      }
      if(e.getSource() == upB) {
        // YLÖSPÄIN SIIRTYMINEN KARTALLA
    	  coordinateChange(4);
    	  try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
      }
      
      if(e.getSource() == downB) {
        // ALASPÄIN SIIRTYMINEN KARTALLA
    	  coordinateChange(5);
    	  try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
      }
      
      if(e.getSource() == zoomInB) {
    	  System.out.println("funkkariin");
    	// ZOOM IN -TOIMINTO
    	  coordinateChange(0);
    	  
    	  try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
    	  
      }
      
      if(e.getSource() == zoomOutB) {        
        // ZOOM OUT -TOIMINTO
    	  coordinateChange(1);
      try { updateImage(); } catch(Exception ex) { ex.printStackTrace(); }
      }
    }
  }
 
  // Valintalaatikko, joka muistaa karttakerroksen nimen
  private class LayerCheckBox extends JCheckBox {
    private String name = "";
    public LayerCheckBox(String name, String title, boolean selected) {
      super(title, null, selected);
      this.name = name;
    }
    public String getName() { return name; }
  }
 
  // Tarkastetaan mitkä karttakerrokset on valittu,
  // tehdään uudesta karttakuvasta pyyntö palvelimelle ja päivitetään kuva
  public void updateImage() throws Exception {
	  String s = "";
 
	  // Tutkitaan, mitkä valintalaatikot on valittu, ja
	  // kerätään s:ään pilkulla erotettu lista valittujen kerrosten
	  // nimistä (käytetään haettaessa uutta kuvaa)
	  Component[] components = leftPanel.getComponents();
	  for(Component com:components) {
		  if(com instanceof LayerCheckBox)
			  if(((LayerCheckBox)com).isSelected()) s = s + com.getName() + ",";
	  }
	  if (s.endsWith(",")) s = s.substring(0, s.length() - 1);
	  GetMap newImage = new GetMap("Update", s);	//Luodaan kartan lataava säie ja välitetään kerroksista koostuva merkkijono sille
	  newImage.start();
 
    // TODO:
    // getMap-KYSELYN URL-OSOITTEEN MUODOSTAMINEN JA KUVAN PÄIVITYS ERILLISESSÄ SÄIKEESSÄ
    // imageLabel.setIcon(new ImageIcon(url));
  }
  
  class GetMap extends Thread{
	  private String layers;	//Kerrosten nimet pilkulla erotettuna
	  private int startPO;		//Kerrosten ensimmäinen indeksi URL:ssä
	  private int endPO;		//Ensimmäinen kerrosten jälkeinen merkki URL:ssä
	  
	  public GetMap(String name, String layers){
		  super(name);
		  this.layers = layers;
		  startPO = getMapUrl.indexOf("LAYERS") + 7;
		  endPO = getMapUrl.indexOf("&STYLES");
	  }
	  
	  @Override
	  public void run(){
		  getMapUrl.replace(startPO, endPO, layers);		//Korvataan URL:ssä aiemmin olleet kerrokset valituilla
		  try{
			  imageLabel.setIcon(new ImageIcon(new URL(getMapUrl.toString())));		//Ladataan uusi karttakuva
		  } catch (Exception e){}
	  }
  }
  
  public void coordinateChange(int i) {
	  // Haetaan BBOX= "="-merkin kohta ja myös sen jälkeinen viimeinen kohta mikä päättyy merkkiin "&" jotta tiedetään koordinaattitietojen alue joita lähdetään muokkaamaan
	  int startPO = getMapUrl.indexOf("BBOX")+5;
	 // int endPO = getMapUrl.lastIndexOf(",",startPO+10);
	  
	  
	  
	 // String pattern = "###.######";
	  Locale currentLocale = Locale.getDefault();
	  DecimalFormatSymbols symbols = new DecimalFormatSymbols(currentLocale);
	  symbols.setDecimalSeparator('.');
	  DecimalFormat dmFor = new DecimalFormat("0.000000", symbols);
	  
	  
	  
	  System.out.println("kerroin: " + kerroin);
	  switch(i) {
	  	case 0: //zoomIn
		  	xMin*=0.8;
		  	xMax*=0.8;
		  	yMin*=0.8;
		  	yMax*=0.8;
		  	kerroin*=0.8;
	  		break;
	  	case 1: //zoomOut
	  		xMin*=1.2;
		  	xMax*=1.2;
		  	yMin*=1.2;
		  	yMax*=1.2;
		  	kerroin*=1.2;
		  	break;
	  	case 2://vasemmalle
	  		xMax-=kerroin;
	  		xMin-=kerroin;
	  		break;
	  	case 3://oikealle
	  		xMax+=kerroin;
	  		xMin+=kerroin;
	  		break;
	  	case 4://up
	  		yMax+=kerroin;
	  		yMin+=kerroin;
	  		break;
	  	case 5://down
	  		yMax-=kerroin;
	  		yMin-=kerroin;
	  		break;
	  	
	  }
	  
	  
	  
	  
	  getMapUrl.replace(startPO, getMapUrl.lastIndexOf("&SRS="), dmFor.format(xMin)+ ","+ dmFor.format(yMin) + "," + dmFor.format(xMax) + "," + dmFor.format(yMax));
	
	  
  }
  


 
} // MapDialog