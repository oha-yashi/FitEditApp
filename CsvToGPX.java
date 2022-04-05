package com.example.fiteditapp;

import android.os.Handler;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

/**
 * ヘッダー要件
 * CsvEdit.MY_COMMENT ...
 * Timestamp,EpochTime,Segment,Latitude[deg],Longitude[deg],...
 */
public class CsvToGPX {
    static boolean csvCheck = false;
    static final String HEADER = "Timestamp,EpochTime,Segment,Latitude[deg],Longitude[deg]";
    // args : [input] (input)...
    public static void main(Handler handler, String[] args){
        for(String path: args) {
            // https://techbooster.org/android/application/7671/
            DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
            try {
                BufferedReader br = new BufferedReader(new FileReader(new File(path)));
                DocumentBuilder db = dbf.newDocumentBuilder();
                Document doc = db.newDocument();

                // xml記述
                // https://www.kunimiyasoft.com/gpx-format/
                // http://tancro.e-central.tv/grandmaster/gpx/yamareco-gpx.html
                Element root = doc.createElement("gpx");
                doc.appendChild(root);

                String text;
                int segNow = CsvEdit.segmentStart;
                Element trkseg = null;
                while((text=br.readLine()) != null){
                    if(text.startsWith(CsvEdit.MY_COMMENT)){
                        csvCheck = true;
                        continue;
                    } // 1行目コメントチェック
                    if(csvCheck && text.startsWith("Timestamp,EpochTime,Segment,Latitude[deg],Longitude[deg]")){
                        continue;
                        // 2行目読み飛ばし
                    }

                    if(!csvCheck){
                        throw new FileNotFoundException("CSV format error");
                    } // csvCheck通ってるはず
                    String[] data = text.split(",");

                    int segNum = Integer.parseInt(data[2]);
                    if(segNow < segNum){
                        // セグメント番号が変わったとき
                        segNow = segNum;
                        Element trk = doc.createElement("trk");
                        root.appendChild(trk);
                        Element num = doc.createElement("number");
                        num.setTextContent(String.valueOf(segNow));
                        trk.appendChild(num);
                        trkseg = doc.createElement("trkseg");
                        trk.appendChild(trkseg);
                        // trkタグを新調する
                    }

                    Element trkpt = doc.createElement("trkpt");
                    trkpt.setAttribute("lat",data[3]);
                    trkpt.setAttribute("lon",data[4]);
                    trkseg.appendChild(trkpt);

                    Element time = doc.createElement("time");
                    long epoch = Long.parseLong(data[1]);
                    time.appendChild(doc.createTextNode(epochToGreenwich(epoch)));
                    trkpt.appendChild(time);
                }

                // gpxファイルに変換・書き出し
                TransformerFactory tf = TransformerFactory.newInstance();
                Transformer transformer = tf.newTransformer();

                String newPath = path.substring(0, path.length() - 4) + ".gpx";
                File gpx = new File(newPath);
                if (!gpx.exists()) {
                    gpx.createNewFile();
                }
                transformer.transform(new DOMSource(doc), new StreamResult(gpx));
                handler.post(()->System.out.println("convert " + path + " to GPX"));
            } catch (ParserConfigurationException | IOException | TransformerException e) {
                handler.post(e::printStackTrace);
            }
        }
    }

    final static SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US);
    public static String epochToGreenwich(long epochTime){
        sdf.setTimeZone(TimeZone.getTimeZone("GMT"));
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(epochTime*1000);
        return sdf.format(c.getTime());
    }
}