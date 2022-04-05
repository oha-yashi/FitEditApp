package com.example.fiteditapp;

import android.os.Handler;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;
import java.util.TimeZone;

import static java.lang.Double.*;

public class CsvEdit {
  final static int fitTimeZero = 631065600;
  final static Double pow2_31 = Math.pow(2,31);
  static boolean addFile = false;
  static boolean isFirstFile = true;
  static double distanceRecord = 0.0;
  static final int segmentChangeLimit = 60; //segment切替になる中断秒数
  static final int segmentStart = -1;
  static final String MY_COMMENT = "#FitEditApp by ohys"; //ここで編集したCSVの冒頭に入れるコメント

  /**
   * @param args [output] [input] (input...)
   * @param shrink > 0
   */
  public static void main(Handler handler, String[] args, GetDistance getDistance, double homeArea, int shrink) throws IOException{
    int lineCount = -1;
    int segmentNum = segmentStart;
    double lon, lat;
    lon = getDistance.getZeroPoint()[0];
    lat = getDistance.getZeroPoint()[1];
    if(shrink<=0)shrink = 1;
    String out = "error.csv";
    int argc = args.length;
    if(argc<2){
      handler.post(()->System.out.println("input needs Output and Input(s)"));
    }else if(argc > 2)addFile = true;

    for(int i=0; i<argc; i++){// for each filenames
      String in = args[i];
      if(i==0 && in.endsWith(".csv")){
        out = in;
        continue;
      }else if(!in.endsWith(".csv")){
        continue;
      }

      File fInput = new File(in);
      File fOutput = new File(out);

      double distanceAlreadyMoved = distanceRecord; // 前のファイルで進んだ距離
      try(BufferedReader br = new BufferedReader(new FileReader(fInput))){
        try(FileWriter fw = new FileWriter(fOutput, !isFirstFile)){
          String finalOut = out;
          handler.post(()->Log("read " + in + " and write to " + finalOut));
          if(isFirstFile){
            // ヘッダー記述
            fw.write(MY_COMMENT+" #shrink="+shrink+"\n"); // コメント
            fw.write("Timestamp,EpochTime,Segment,Latitude[deg],Longitude[deg],Distance[km],Altitude[m],Speed[km/h]\n");
            if(addFile){
              isFirstFile = false;
            }
          }

          String text;
          int timeBefore = 0;
          while((text=br.readLine()) != null){
            // 1行読み込む
            String[] data = text.split(",");

            lineCount++; //-1始まりなので最初に0になる
            if(data[0].equals("Data") && data[1].equals("8") && data[2].equals("record")){
              // 読み込めるデータ全件チェックする
              int epochTime;

              StringBuilder sb = new StringBuilder();

              int n = 3;
              while(n < data.length){ // 横列を進んで各項目を取得
                switch (data[n]) {
                  case "timestamp":
                    n++;
                    String ts = data[n].split("\"")[1];
                    epochTime = Integer.parseInt(ts) + fitTimeZero;
                    if(epochTime - timeBefore > segmentChangeLimit){
                      // 1秒ごとのはずなので、Limit秒途切れていたらセグメント切替
                      segmentNum++;
                      // -1始まりなので、最初に0になる
                    }
                    timeBefore = epochTime; // セグメント切替有無に関わらず更新
                    sb.append(epochToStr(epochTime)).append(",")
                            .append(epochTime).append(",")
                            .append(segmentNum);
                    break;
                  case "position_lat":
                    n++;
                    String Lat = data[n].split("\"")[1];
                    lat = parseDouble(Lat) * 180 / pow2_31;
                    sb.append(",").append(myDouble(lat));
                    break;
                  case "position_long":
                    n++;
                    String Lon = data[n].split("\"")[1];
                    lon = parseDouble(Lon) * 180 / pow2_31;
                    sb.append(",").append(myDouble(lon));
                    break;
                  case "distance":
                    n++;
                    double dd = parseDouble(data[n].split("\"")[1]) / 1000;
                    distanceRecord = dd;
                    sb.append(",").append(myDouble(dd + distanceAlreadyMoved));
                    break;
                  case "altitude":
                    n++;
                    sb.append(",").append(data[n].split("\"")[1]);
                    break;
                  case "speed":
                    n++;
                    sb.append(",").append(myDouble(parseDouble(
                            data[n].split("\"")[1]) * 3.6));
                    break;
                }
                n++;
              }
              if(getDistance.calculate(lat,lon) > homeArea
                      && lineCount % shrink == 0
              ) {// 読み込み頻度 1/shrink
                fw.write( sb.append("\n").toString() );
              }
            }
          }// for each line

          handler.post(()->Log("End reading"));
        }
      }
    }// for each files
    handler.post(()->Log("End all convert"));
  }

  static SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US);
  private static String epochToStr(int time){
    dateFormat.setTimeZone(TimeZone.getTimeZone("Asia/Tokyo"));
    Calendar c = Calendar.getInstance();
    c.setTimeInMillis((long)time *1000);
    return dateFormat.format(c.getTime());
  }
  static String myDouble(double db){
    return BigDecimal.valueOf(db).toPlainString();
  }

  static void Log(String s){System.out.println(s);}
}