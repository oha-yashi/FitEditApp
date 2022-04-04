package com.example.fiteditapp;

import static java.lang.Math.pow;
import static java.lang.Math.sqrt;
import static java.lang.Math.toRadians;

/**
 * 緯度経度のセットから基準点->測定点の距離(km)を算出する。ヒュベニの公式
 * 基準点(p1, q1)->測定点(p2, q2)
 * distance = new GetDistance(p1, q1).calculate(p2, q2);
 * https://www.gis-py.com/entry/py-latlon2distance
 */
public class GetDistance {
    private final double Rx = 6378137.0; // 長半径
    private final double Ry = 6356752.314245; // 短半径
    private final double e2 = (pow(Rx,2) - pow(Ry,2))/ pow(Rx,2); // 離心率の2乗
    private final double latitude_zero; // 基準点の緯度
    private final double longitude_zero; // 基準点の経度

    GetDistance(double lat, double lon){
        latitude_zero = lat; // ラジアンで扱う
        longitude_zero = lon;
    }

    public double[] getZeroPoint(){
        return new double[]{latitude_zero, longitude_zero};
    }

    public double calculate(double lat, double lon){
        lat = toRadians(lat); // 測定点の緯度
        lon = toRadians(lon); // 測定点の経度
        double lat_dif = lat - toRadians(latitude_zero); // 緯度差
        double lon_dif = lon - toRadians(longitude_zero); // 経度差
        double lat_ave = (lat + toRadians(latitude_zero)) / 2; //緯度平均

        double w = sqrt(1 - (e2 * pow(Math.sin(lat_ave),2))); // 子午線・卯酉線曲率半径の分母
        double m = Rx * (1-e2) / pow(w,3); // 子午線曲率半径
        double n = Rx / w; // 卯酉線曲率半径

        double distance = sqrt(pow(lat_dif * m, 2) + pow(lon_dif * n * Math.cos(lat_ave), 2));
        return distance/1000;
    }
}
