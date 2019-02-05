package com.fahim.servertest.Calculation;

import android.content.Context;
import android.content.res.AssetManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;


import com.fahim.servertest.CalculationCallback;

import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Random;

public class PpgMainClass implements Runnable {

    private  Context context;
    private double[] ppgSignal1;
    private double[] ppgSignal2 ;
    private double[] accDataX ;
    private double[] accDataY ;
    private double[] accDataZ ;
    private int lParameterOfRLS;
    private int fSampling ;
    public double fPrev;
    private double freqEstimates;

    private Handler handler;

    private int identifier;

    private CalculationCallback callback;
    public PpgMainClass(Context ctx,CalculationCallback callback,int v){

        this.setContext(ctx);
        this.setParameterOfRls(40);
        this.fSampling = 100; // 125;
        this.fPrev = 0;

        this.callback = callback;

        identifier = v;

    }

    public void setData(double[] p1, double[] p2, double[] ax, double[] ay, double[] az){

        this.ppgSignal1 = p1;
        this.ppgSignal2 = p2;
        this.accDataX = ax;
        this.accDataY = ay;
        this.accDataZ = az;

    }

    public void sentDataToHandler(double val){
        handler.post(() -> callback.onCompleteCalculation(identifier,val));
    }

    public void setHandler(Handler h){
        handler = h;
    }

    private void setContext(Context ctx){
        this.context = ctx;
    }

    private void setParameterOfRls(int x){
        this.lParameterOfRLS = x;
    }

    @Override
    public void run() {
        try {
            calcMain();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public boolean checkIfData(double[] sig){

        int N = (int) Math.pow(2,10);
        int strBpm = 0;
        int endBpm = 200;
        double threshold = 0.4;

        int[] locs = getLocFromFreqz(sig, strBpm, endBpm, threshold ,fSampling , N);
        double[] freqs = w(locs,N,strBpm,fSampling);

        for (double f : freqs) {
            if ( f > 50 && f < 150 ){
                return true;
            }
        }
        return false;
    }


    private synchronized void calcMain() throws IOException {

        int multiplier = fSampling / 125;
        freqEstimates = -1;

        double[] ppgSignalAverage = DSP.times(DSP.plus(ppgSignal1, ppgSignal2), 0.5);
        double[] ex = rlsFilter(lParameterOfRLS, accDataX, ppgSignalAverage);

        double[] exy = rlsFilter(lParameterOfRLS, accDataY, ex);
        double[] rRaw = rlsFilter(lParameterOfRLS, accDataZ, exy);


        /**
         * filtering all data to frequency range for human HR
         */
        double[] rN = myBandPass(rRaw, fSampling, 1);

        if( fPrev <= 0 ){
            fPrev = initialize( rN, accDataX,accDataY, accDataZ,fSampling);

            this.sentDataToHandler(fPrev);
            return;
            //return fPrev;
        }

        double[] accDataXfiltered = myBandPass(accDataX, fSampling, 1);
        double[] accDataYfiltered = myBandPass(accDataY, fSampling, 1);
        double[] accDataZfiltered = myBandPass(accDataZ, fSampling, 1);

        double[] ppgSignal1S = myBandPass(ppgSignal1, fSampling, 2);
        double[] ppgSignal2S = myBandPass(ppgSignal2, fSampling, 2);

        double[] accDataXS = myBandPass(accDataX, fSampling, 2);
        double[] accDataYS = myBandPass(accDataY, fSampling, 2);
        double[] accDataZS = myBandPass(accDataZ, fSampling, 2);



        double freq_td = frequency_estimate(
                ppgSignal1S,
                ppgSignal2S,
                accDataXS,
                accDataYS,
                accDataZS,
                fPrev, multiplier);

        double[] yCropped = rN;


        double[] S_rls = maxFindFromThreshold(yCropped, 0.6, fSampling);

        ArrayList<Double> S_a = new ArrayList<>();

        double[] dominantPeaks = maxFindFromThreshold(accDataXfiltered, 0.6, fSampling);
        if (dominantPeaks.length > 2) {
            extendList(S_a, subList(dominantPeaks, 2));
        } else {
            extendList(S_a, dominantPeaks);
        }
        dominantPeaks = maxFindFromThreshold(accDataYfiltered, 0.6, fSampling);
        if (dominantPeaks.length > 2) {
            extendList(S_a, subList(dominantPeaks, 2));
        } else {
            extendList(S_a, dominantPeaks);
        }
        dominantPeaks = maxFindFromThreshold(accDataZfiltered, 0.6, fSampling);
        if (dominantPeaks.length > 2) {
            extendList(S_a, subList(dominantPeaks, 2));
        } else {
            extendList(S_a, dominantPeaks);
        }

        ArrayList<Double> fRlsSet = new ArrayList<>();
        for (double iRLS : S_rls) {
            if (S_a.size() > 0)
                if (DSP.min(DSP.abs(DSP.minus(S_a, iRLS))) > 3) {
                    fRlsSet.add(iRLS);
                }
        }

        double[] fRlsSet_ = filterArray(fRlsSet, 40, 200);

        if (fRlsSet_.length == 1 && Math.abs(DSP.min(DSP.minus(fRlsSet_, fPrev))) < 30) {
            freqEstimates = fRlsSet_[0];
        } else if (freq_td != -1 && Math.abs(freq_td - fPrev) < 12) {
            freqEstimates = freq_td;
        } else {
            double[] minVal = minValLoc(DSP.abs(DSP.minus(S_rls, fPrev)));
            double min = minVal[0];
            int pos = (int) (minVal[1]);
            if (min < 9) {
                freqEstimates = S_rls[pos];
            }

            if (freqEstimates == -1) {
                double S_org = findSignalPeaks(
                        ppgSignal1S,
                        ppgSignal2S,
                        fPrev, 5, fSampling
                );

                double S_a0 = findSignalPeaks(
                        accDataXS,
                        accDataYS,
                        accDataZS,
                        fPrev, 10, fSampling
                );


                if (Math.abs(S_a0 - S_org) > 3) {
                    freqEstimates = S_org;
                } else {
                    freqEstimates = fPrev;
                }

            }


        }

        this.sentDataToHandler(freqEstimates);

        fPrev =  callNLMS( ppgSignal1,
                ppgSignal2, freqEstimates, 4, fSampling)  ;

        this.sentDataToHandler(fPrev);

        freqEstimates = fPrev;
        // return freqEstimates;

    }



    public double getFreq(){
        return freqEstimates;
    }


    public  void m_main() throws IOException {
        // TODO code application logic here

        int fSampling = 125;
        int multiplier = fSampling/125;

        Log.d("START","calculation started");



        Log.d("DATA RETRIEVAL","Data is recieved");

        long  strtTime = System.nanoTime();

        double[] ppgSignalAverage = DSP.times( DSP.plus(ppgSignal1, ppgSignal2) , 0.5) ;

        /**
         * RLS filtering
         */


        double[] ex = rlsFilter(lParameterOfRLS, accDataX, ppgSignalAverage);

        double[] exy = rlsFilter(lParameterOfRLS, accDataY, ex);
        double[] rRaw = rlsFilter(lParameterOfRLS, accDataZ, exy);


        /**
         * filtering all data to frequency range for human HR
         */
        double[] rN = myBandPass(rRaw, fSampling,1);
        double[] accDataXfiltered = myBandPass(accDataX, fSampling,1);
        double[] accDataYfiltered = myBandPass(accDataY, fSampling,1);
        double[] accDataZfiltered = myBandPass(accDataZ, fSampling,1);

        double fPrev = initialize( subList(rN,1000), subList(accDataX,1000),subList(accDataY,1000),
                subList(accDataZ, 1000),fSampling);

        // bandpassing all the data of signal
        double[] ppgSignal1S = myBandPass(ppgSignal1,fSampling,2);
        double[] ppgSignal2S = myBandPass(ppgSignal2,fSampling,2);

        double[] accDataXS = myBandPass(accDataX,fSampling,2);
        double[] accDataYS = myBandPass(accDataY,fSampling,2);
        double[] accDataZS = myBandPass(accDataZ,fSampling,2);

        double deltaCount = 0;
        // now doing the emd portion
        int iStart = 0;
        int iStep  = 250 * multiplier ;
        //int iStop  = rN.length - 1000 * multiplier - 1 ;
        int iStop  = rN.length - 1000 * multiplier  ;

        boolean isDebug = false;
        boolean isPrint = false;

        double delta_count = 0 ; // need in EEMD


        int iCounter = 1;
        int iSegment = iStart;

        while(iSegment<=iStop){

//            if (iCounter==42){
//                System.out.println("|");
//            }

            if(isDebug){
                isDebug = false;
                iCounter = 62;
                fPrev = 144.05;
                iSegment = iStart + (iCounter-1) * iStep ;
                isPrint = true;
            }

            // TODO: check
            int[] currentSegment = DSP.irange(iSegment, iSegment + 1000 * multiplier - 1);

            //Double[] peaks_ = new Double[0];
            ArrayList<Double> peaks_ = new ArrayList<>();

            double freqEstimates = -1; /*doEEMD(
                    getValuesFromIndex(ppgSignal1S, currentSegment),
                    getValuesFromIndex(ppgSignal2S, currentSegment),
                    getValuesFromIndex(accDataXS, currentSegment),
                    getValuesFromIndex(accDataYS, currentSegment),
                    getValuesFromIndex(accDataZS, currentSegment),
                    fPrev, deltaCount ,fSampling,peaks_
            );*/


            // we construct Simf \3 Sa, 0.5, and from this set, we
            // take the peak location nearest to fprev
            double[] peaks = toDoubleArray(peaks_);

            double [] tmp = DSP.abs( DSP.min(peaks, fPrev) );

            double[] minLoc = minValLoc( tmp );
            int loc = (int)minLoc[1];
            double minimum ;
            if(loc==-1){
                minimum = -1;
            }
            else{
                minimum = minLoc[0];

            }

            double freq_td = frequency_estimate(
                    getValuesFromIndex(ppgSignal1S, currentSegment),
                    getValuesFromIndex(ppgSignal2S, currentSegment),
                    getValuesFromIndex(accDataXS, currentSegment),
                    getValuesFromIndex(accDataYS, currentSegment),
                    getValuesFromIndex(accDataZS, currentSegment),
                    fPrev, multiplier);

            // if(isPrint)
            //    System.out.println("freq_td : "+freq_td);

            if(freqEstimates!=-1){
                //tracking from AC
                delta_count = 0;
                //System.out.println("tracking from AC");
            }
            else if( minimum!=-1 &&  minimum <= 9 ){
                //If its distance from fprev is within 7 BPM
                // tracking from emd 2
                freqEstimates = peaks[loc];
                if (delta_count>0){
                    delta_count = delta_count - 0.5;
                }
                //System.out.println("('tracking from emd :");
            }
            else{
                // track from rls
                delta_count = delta_count + 1;

                double[] yCropped = getValuesFromIndex(rN, currentSegment);

                // we put its dominant peaks (80% of the maximum) in
                // Srls
                double[] S_rls = maxFindFromThreshold(yCropped,0.6,fSampling);
//
//                if(isPrint){
//                    //System.out.println("S_rls : ");
//                    printList(S_rls);
//                }

                ArrayList<Double> S_a = new ArrayList<>();

                double[] dominantPeaks = maxFindFromThreshold(getValuesFromIndex(accDataXfiltered, currentSegment), 0.6, fSampling);
                if(dominantPeaks.length>2){
                    extendList(S_a, subList(dominantPeaks, 2));
                }else{
                    extendList(S_a, dominantPeaks);
                }
                dominantPeaks = maxFindFromThreshold(getValuesFromIndex(accDataYfiltered, currentSegment), 0.6, fSampling);
                if(dominantPeaks.length>2){
                    extendList(S_a, subList(dominantPeaks, 2));
                }else{
                    extendList(S_a, dominantPeaks);
                }
                dominantPeaks = maxFindFromThreshold(getValuesFromIndex(accDataZfiltered, currentSegment), 0.6, fSampling);
                if(dominantPeaks.length>2){
                    extendList(S_a, subList(dominantPeaks, 2));
                }else{
                    extendList(S_a, dominantPeaks);
                }

                if(isPrint){
                    System.out.println(" S_a  : ");
                    printList(S_a);
                }
                // set Srls\3 Sa,0.6
                ArrayList<Double> fRlsSet = new ArrayList<>();
                for (double iRLS : S_rls) {
                    if(S_a.size()>0)
                        if( DSP.min( DSP.abs( DSP.minus(S_a, iRLS) ) ) > 3 ){
                            fRlsSet.add(iRLS);
                        }
                }

                double[] fRlsSet_ = filterArray(fRlsSet, 40, 200);

//                if(isPrint){
//                    System.out.println("f_rls :"  );
//                    printList(fRlsSet_);
//                }
                if( fRlsSet_.length == 1 &&  Math.abs( DSP.min( DSP.minus(fRlsSet_, fPrev) ) ) < 30 ){
                    freqEstimates = fRlsSet_[0];
                }
                else if( freq_td != -1 && Math.abs( freq_td - fPrev ) < 12 ){
                    freqEstimates = freq_td;
                }
                else{
                    double[] minVal  =  minValLoc(DSP.abs( DSP.minus(S_rls, fPrev) ) );
                    double min = minVal[0];
                    int pos = (int)(minVal[1]);
                    if ( min < 9 ){
                        freqEstimates = S_rls[pos];
                    }

                    if(freqEstimates == -1){
                        double S_org = findSignalPeaks(
                                getValuesFromIndex(ppgSignal1S, currentSegment),
                                getValuesFromIndex(ppgSignal2S, currentSegment),
                                fPrev,5,fSampling
                        );

                        double S_a0 = findSignalPeaks(
                                getValuesFromIndex(accDataXS, currentSegment),
                                getValuesFromIndex(accDataYS, currentSegment),
                                getValuesFromIndex(accDataZS, currentSegment),
                                fPrev,10,fSampling
                        );


                        if( Math.abs(S_a0 - S_org) > 3 ){
                            freqEstimates = S_org;
                        }else{
                            freqEstimates = fPrev;
                        }

                    }

                }

            }
            // TODO:  call_nlms2
            fPrev =  callNLMS( getValuesFromIndex(ppgSignal1, currentSegment),
                    getValuesFromIndex(ppgSignal2, currentSegment), freqEstimates, 4, fSampling)  ;

            //System.out.println(iCounter+" : "+fPrev);



            long elapsedTime = System.nanoTime() - strtTime;

            //System.out.println("Elapsed time : " + elapsedTime/1000000000);

            iSegment = iSegment + iStep;
            iCounter = iCounter + 1;

            //return ;


        }


    }

    /**
     * Display list
     * @param l ArrayList
     */
    public static void printList(ArrayList<Double> l   ){

        int len = l.size();
        for(int i=0;i<len;i++){
            System.out.println(i + " : "+ l.get(i));
        }
        System.out.println("");

    }

    public static void printList(double[] l   ){

        int len = l.length;
        for(int i=0;i<len;i++){
            System.out.println(i + " : "+ l[i]);
        }

        System.out.println("");
    }


    public static double findSignalPeaks(double [] signal1,double [] signal2,  double fPrev,double range, double fSampling ){

        // double[] w = linspace(fPrev-range, fPrev+range, 100);
        // double[] ww = DSP.times(w, 2 * Math.PI / (fSampling * 60 ) );

        int N = (int)Math.pow(2, 17);

        int strBpm = (int)(fPrev - range);
        int endBpm = (int)(fPrev + range);

        // double[] fy1 = DSP.power(absFreqz(signal1, ww) , 2 );
        // double[] fy2 = DSP.power(absFreqz(signal2, ww) , 2 );
        int[] loc1 = getLocFromFreqz(signal1, strBpm, endBpm, fSampling, N);

        int[] loc2 = getLocFromFreqz(signal2, strBpm, endBpm, fSampling, N);


        int[] pksLoc = concateArray(loc1, loc2);

        double[] cand = w(pksLoc, N, strBpm, (float)fSampling);


        if( cand.length!=0 ){
            int pos = (int)(minValLoc( DSP.abs( DSP.minus(cand, fPrev) ) ) )[1];
            return cand[pos];
        }
        else
            return fPrev;

    }


    public static double findSignalPeaks(double [] signal1,double [] signal2,double[] signal3, double fPrev,double range, double fSampling ){

        int N = (int)Math.pow(2, 12);

        int strBpm = (int)(fPrev - range);
        int endBpm = (int)(fPrev + range);

        // double[] fy1 = DSP.power(absFreqz(signal1, ww) , 2 );
        // double[] fy2 = DSP.power(absFreqz(signal2, ww) , 2 );
        int[] loc1 = getLocFromFreqz(signal1, strBpm, endBpm, fSampling, N);

        int[] loc2 = getLocFromFreqz(signal2, strBpm, endBpm, fSampling, N);

        int[] loc3 = getLocFromFreqz(signal3, strBpm, endBpm, fSampling, N);

        int[] pksLoc = concateArray(concateArray(loc1, loc2),loc3);

        double[] cand = w(pksLoc, N, strBpm, (float)fSampling);


        if( cand.length!=0 ){
            int pos = (int)(minValLoc( DSP.abs( DSP.minus(cand, fPrev) ) ) )[1];
            return cand[pos];
        }
        else
            return fPrev;

    }

    /**
     *
     * @param signal1
     * @param signal2
     * @param freq
     * @param range
     * @param fSampling
     * @return
     */
    public static double callNLMS(double[] signal1, double[] signal2,double freq, double range,double fSampling){

        // double[] w = linspace(freq-range, freq+range, 4000);
        // double[] ww = DSP.times(w, 2 * Math.PI / (fSampling * 60));

        int N = (int)Math.pow(2, 12);

        int strBpm = (int)(freq - range);
        int endBpm = (int)(freq + range);

        int[] loc1 = getLocFromFreqz(signal1, strBpm, endBpm, fSampling, N);

        int[] loc2 = getLocFromFreqz(signal2, strBpm, endBpm, fSampling, N);

        int[] pksLoc = concateArray(loc1, loc2);

        double[] cand = w(pksLoc, N, strBpm, (float)fSampling);

        if( cand.length!=0 ){
            int pos = (int)(minValLoc( DSP.abs( DSP.minus(cand, freq) ) ) )[1];
            return cand[pos];
        }
        else
            return freq;

    }

    /**
     *
     * @param a
     * @param min
     * @param max
     * @return
     */
    public static double[] filterArray(double[] a, double min, double max){

        ArrayList<Double> filteredArray = new ArrayList<>();
        for (double val : a) {
            if( val >= min && val <= max  ){
                filteredArray.add(val);
            }
        }

        return toDoubleArray(filteredArray);

    }

    /**
     * TODO:::::::::
     * @param sig1
     * @param sig2
     * @param accX
     * @param accY
     * @param accZ
     * @param fPrev
     * @param multiplier
     * @return
     */
    public static double frequency_estimate(double[] sig1, double[] sig2, double[] accX,
                                            double[] accY, double[] accZ,double fPrev,int multiplier){

        int N = (int)Math.pow(2, 17);

        int strBpm = 0;
        int endBpm = 200;

        float fSampling = multiplier * 125;

        double freqEst1 = -1;
        double freqEst2 = -1;

        double[] ch1 = timeDomain(sig1,accX,accY,accZ,fPrev,multiplier);
        double[] ch2 = timeDomain(sig2,accX,accY,accZ,fPrev,multiplier);

        if(ch1!=null){
            //double[] w = linspace(0, 200, 1000);
            //double[] ww = DSP.times(w, 2 * Math.PI / (125 * 60 * multiplier) );

            // double[] yy1 = DSP.power(absFreqz(ch1, ww) , 2 );


            int[] loc = getLocFromFreqz(ch1, strBpm, endBpm, fSampling, N);

            double[] w_ = w(loc, N, strBpm, fSampling);

            //System.err.println(w_.length);


            int minloc = (int)(minValLoc( DSP.abs( DSP.minus(w_, fPrev) ) ) )[1];

            freqEst1 = w_[minloc];

        }
        if(ch2!=null){
            //double[] w = linspace(0, 200, 1000);
            // double[] ww = DSP.times(w, 2 * Math.PI / (125 * 60 * multiplier) );

            // double[] yy1 = DSP.power(absFreqz(ch2, ww) , 2 );

            // int[] loc = findPeakLocation(yy1);

            int[] loc = getLocFromFreqz(ch2, strBpm, endBpm, fPrev, N);

            double[] w_ = w(loc, N, strBpm, fSampling);

            int minloc = (int)(minValLoc( DSP.abs( DSP.minus(w_, fPrev) ) ) )[1];

            //double[] w_ = getValuesFromIndex(w, loc);

            double[] ww_ = w(loc, N, strBpm, fSampling);

            int minloc_ = (int)(minValLoc( DSP.abs( DSP.minus(ww_, fPrev))))[1];

            freqEst2 = ww_[minloc_];

        }

        double freqEst = -1;

        double bd = 12;

        if(freqEst1 == -1 || freqEst2==-1 ){
            return -1;
        }

        double d1 = Math.abs( freqEst1 - fPrev );
        double d2 = Math.abs( freqEst2 - fPrev );

        if( d1 <= bd && d2 <= bd ){
            if( d1 <= d2 ){
                freqEst = freqEst1;
            }else{
                freqEst = freqEst2;
            }
        }
        else if( d1 <= bd ){
            freqEst = freqEst1;
        }
        else{
            freqEst = freqEst2;
        }


        return freqEst;
    }

    /**
     *
     * @param list
     * @return
     */
    public static double[] minValLoc(double[] list){
        int minLoc = -1;
        double minVal = Double.MAX_VALUE;
        for(int i=0;i<list.length;i++){
            if(list[i] < minVal){
                minVal = list[i];
                minLoc = i;
            }
        }
        return new double[]{minVal,(double)minLoc};

    }

    public static double[] awgn(double[] sig, double SNR){

        int len = sig.length;
        //double[] asig = new double[len];
        double std = Math.sqrt(  powerOfSignal(sig) / Math.pow(10, SNR/10.0));
        //System.out.println("STD:"+std);
        double[] noise = DSP.times( randn(len,0) , std ) ;
        // System.out.print("awgn:");
        // System.out.println(powerOfSignal(sig));
        // DSP.printArray( noise );
        return DSP.plus(sig, noise);
    }

    /**
     *
     * @param len
     * @return
     */
    public static double[] randn(int len){

        Random r = new Random();
        double[] rn = new double[len];
        for (int i = 0; i < len; i++) {
            rn[i] = r.nextGaussian();
        }
        return rn;
    }

    /**
     *
     * @param len
     * @param seed
     * @return
     */
    public static double[] randn(int len,int seed){

        Random r = new Random(seed);
        double[] rn = new double[len];
        for (int i = 0; i < len; i++) {
            rn[i] = r.nextGaussian();
        }
        return rn;
    }

    /**
     *
     * @param sig
     * @return
     */
    public static double powerOfSignal(double[] sig){
        int len = sig.length;
        double sum = 0;
        for (double d : sig) {
            sum += d*d;
        }
        return sum / len;
    }

    public static Double[] toDouble(double[] d){
        Double[] n = new Double[d.length];

        for (int i = 0; i < d.length; i++) {
            n[i] = d[i];
        }

        return n;
    }

    public static double[] todouble(Double[] d){
        double[] n = new double[d.length];

        for (int i = 0; i < d.length; i++) {
            n[i] = d[i];
        }

        return n;
    }

    /**
     * TODO::::::::
     * @param sig1
     * @param sig2
     * @param accX
     * @param accY
     * @param accZ
     * @param fPrev
     * @param fSampling
     * @param peaks
     * @return
     */
    public static double doEEMD( double[] sig1, double[] sig2, double[] accX,
                                 double[] accY, double[] accZ, double fPrev, double deltaCount ,double fSampling, ArrayList<Double> peaks){

        double freqEstimates = -1;
        int NE = 8;
        double SNR = 30;
        int imfToChose;

        if(fSampling==25)
            imfToChose = 1;
        else if(fSampling==125)
            imfToChose = 2;
        else if(fSampling==250)
            imfToChose = 3;
        else if(fSampling==500)
            imfToChose = 4;
        else
            imfToChose = 0;

        double[] imf1 = new double[sig1.length];
        double[] imf2 = new double[sig2.length];

        for (int i = 0; i < imf1.length; i++) {
            imf1[i] = 0;
            imf2[i] = 0;
        }


        for (int i = 0; i < NE; i++) {

            double[] sigWithNoise = awgn(sig1, SNR);
            imf1 = DSP.plus( imf1 ,  nwem(sigWithNoise,imfToChose) );

            sigWithNoise = awgn(sig2, SNR);
            imf2 = DSP.plus( imf2 ,  nwem(sigWithNoise,imfToChose) );

        }

        // imf1 = DSP.times( imf1 , 1.0 / imf1.length );
        // imf2 = DSP.times( imf2 , 1.0 / imf2.length );

        double[] S_imf = new double[2];
        S_imf[0] = maxFind( imf1, fSampling );
        S_imf[1] = maxFind( imf2, fSampling );

        double threshold = 0.5;
        ArrayList<Double> S_a = new ArrayList<>();

        double[] pksVector = maxFindFromThreshold( accX, threshold, fSampling );
        if(pksVector!=null &&  pksVector.length > 3 ){
            pksVector = subList(pksVector, 3);
        }

        extendList(S_a, pksVector);

        pksVector = maxFindFromThreshold( accY, threshold, fSampling );
        if(pksVector!=null &&  pksVector.length > 3 ){
            pksVector = subList(pksVector, 3);
        }
        extendList(S_a, pksVector);

        pksVector = maxFindFromThreshold( accZ, threshold, fSampling );
        if(pksVector!=null &&  pksVector.length > 3 ){
            pksVector = subList(pksVector, 3);
        }
        extendList(S_a, pksVector);


        ArrayList<Double> S_imf_a_3 = new ArrayList<>();
        ArrayList<Double> G2 = new ArrayList<>();

        if(S_a.size() != 0)
           /* for (int i = 0; i < S_imf.length; i++) {

                if( DSP.min( DSP.abs( DSP.minus( S_a ,  S_imf[i] ) ) ) > 3 ){
                    S_imf_a_3.add( S_imf[i] );
                }

                if( DSP.min( DSP.abs( DSP.minus( S_a ,  S_imf[i] ) ) ) > 2 ){
                    G2.add( S_imf[i] );
                }

            }*/

            for( double s : S_imf){
                if( DSP.min( DSP.abs( DSP.minus( S_a ,  s ) ) ) > 3 ){
                    S_imf_a_3.add( s );
                }

                if( DSP.min( DSP.abs( DSP.minus( S_a ,  s ) ) ) > 2 ){
                    G2.add( s );
                }

            }

        // peaks = toDouble(toDoubleArray(S_imf_a_3) );
        for(int i=0;i<S_imf_a_3.size();i++){
            peaks.add( S_imf_a_3.get(i) );
        }

        if( G2.size()==0 ){
            return freqEstimates;
        }

        double f_AC;

        if( DSP.max(G2) - DSP.min(G2) <= 2 ){
            f_AC = DSP.mean(G2);
        }else{
            int loc = (int)(minValLoc( DSP.abs( DSP.minus(G2, fPrev))))[1];
            f_AC = G2.get(loc);
        }

        double delta_0 = 5;
        double delta_d = 1;

        double delta_AC = delta_0 + deltaCount * delta_d;

        if( Math.abs(f_AC - fPrev) < delta_AC ){
            freqEstimates = f_AC;
        }

        return freqEstimates;
    }


    /**
     *
     * @param doubleArray
     * @param len
     * @return
     */
    public static double[] subList(double[] doubleArray, int len){
        double[] arr = new double[len];
        System.arraycopy(doubleArray, 0, arr, 0, len);
        return arr;
    }

    public static double[] subList(double[] doubleArray, int strIndex,int endIndex){
        int len = endIndex - strIndex + 1;
        double[] arr = new double[len];

        for (int i = 0; i < len; i++) {
            arr[i] = doubleArray[strIndex+i];
        }

        // System.arraycopy(doubleArray,strIndex,arr,0,len);

        return arr;
    }


    public static double w(double loc, int N, int strBpm ,float fSampling){
        return loc / N * (60 * fSampling) + strBpm;
    }

    public static double[] w(double[] locs, int N, int strBpm,float fSampling){

        double[] arr = new double[locs.length];
        for (int i = 0; i < locs.length; i++) {
            arr[i] = w(locs[i],N, strBpm,fSampling);

        }
        return arr;

    }
    public static double[] w(int[] locs, int N,int strBpm ,float fSampling){

        double[] arr = new double[locs.length];
        for (int i = 0; i < locs.length; i++) {
            arr[i] = w(locs[i],N, strBpm,fSampling);

        }
        return arr;

    }





    public static int[] getLocFromFreqz(double[] sig, int strBpm, int endBpm, double threshold,double fSampling ,int N){

        double[] fSig = absFreqz(sig,N);

        int strtIndex = new Double( strBpm / (60*fSampling)* N ).intValue();
        int endIndex = new Double( endBpm / (60*fSampling)* N ).intValue();

        fSig = subList(fSig, strtIndex, endIndex);

        return findPeakLocation(fSig, threshold * DSP.max(fSig));

    }

    public static int[] getLocFromFreqz(double[] sig, int strBpm, int endBpm, double threshold,double min,double fSampling ,int N){

        double[] fSig = absFreqz(sig,N);

        int strtIndex = new Double( strBpm / (60*fSampling)* N ).intValue();
        int endIndex = new Double( endBpm / (60*fSampling)* N ).intValue();

        fSig = subList(fSig, strtIndex, endIndex);

        return findPeakLocation(fSig, threshold * DSP.max(fSig),min);

    }




    public static int[] getLocFromFreqz(double[] sig, int strBpm, int endBpm, double threshold,double fSampling ){
        int N = (int)Math.pow(2, 17);
        return getLocFromFreqz(sig, strBpm, endBpm, threshold, fSampling,N);
    }

    public static int[] getLocFromFreqz(double[] sig, int strBpm, int endBpm,double fSampling ,int N){
        double[] fSig = absFreqz(sig,N);

        int strtIndex = new Double( strBpm / (60*fSampling)* N ).intValue();
        int endIndex = new Double( endBpm / (60*fSampling)* N ).intValue();

        if( endIndex>fSig.length ){
            endIndex = fSig.length - 1;
        }

        fSig = subList(fSig, strtIndex, endIndex);

        return findPeakLocation(fSig);

    }


    public static double maxIndexOfFreqz(double[] sig, int strBpm, int endBpm ,double fSampling,int N){

        double[] fSig = absFreqz(sig,N);

        int strtIndex = new Double( strBpm / (60*fSampling)* N ).intValue();
        int endIndex = new Double( endBpm / (60*fSampling)* N ).intValue();

        fSig = subList(fSig, strtIndex, endIndex);

        int loc = maxIndex(fSig);

        return w(loc, N, strBpm, (float)fSampling);
    }


    /**
     * TODO
     * @param sig PPG signal data
     * @param accX
     * @param accY
     * @param accZ
     * @param fSampling
     * @return
     */

    public static double initialize(double[] sig, double[] accX, double[] accY,
                                    double[] accZ, float fSampling){

        double fPrev = -1;

        int N = (int)Math.pow(2, 10);
        //double[] w = linspace(50,150,4096);

        //double[] ww = DSP.times( w, 2 * Math.PI /(fSampling * 60.0) );

        /*int N = (int)Math.pow(2, 17);
        double[] fSig = absFreqz(sig,N);

        // get indices
        int strtIndex = (int)( 50 / (60*fSampling)* N );
        int endIndex = (int)( 150 / (60*fSampling)* N );

        fSig = subList(fSig, strtIndex, endIndex);

        int[] locs = findPeakLocation(fSig, 0.8 * DSP.max(fSig));*/

        int strBpm = 50;
        int endBpm = 150;
        double threshold = 0.8;

        int[] locs = getLocFromFreqz(sig, strBpm, endBpm, threshold ,fSampling , N);

        if(locs.length == 1){
            fPrev = w(locs[0],N,strBpm,fSampling);
            return fPrev;
        }

        //  if more than one frequencies have value above 80%
        //  then we look for their second harmonics

        double[] peakFreqs = w(locs,N,strBpm,fSampling);
        double secondHarmonicMax = -1;

        for(int i = 0; i < locs.length; i++ ){
            for( int j=0; j < locs.length; j++ ){
                if( i != j && Math.abs( 2 * peakFreqs[i] - peakFreqs[j] ) < 5 ){
                    if (peakFreqs[i] > secondHarmonicMax){
                        secondHarmonicMax = peakFreqs[i];
                    }
                }
            }
        }

        if(secondHarmonicMax != -1){  // |2x?y| < 5 BPM found
            fPrev = secondHarmonicMax;
            return fPrev;
        }

        //  taking all the dominant peaks (80% of the maximum peak)
        //  from the acceleration signals

        ArrayList<Double> dominantPeaksOfAcc = new ArrayList<>();

       /* double[] fAccSig = absFreqz(accX);
        locs = findPeakLocation(fAccSig, 0.8 * DSP.max(fAccSig));
        extendList(dominantPeaksOfAcc, w(locs,N,fSampling) );

        fAccSig = absFreqz(accY);
        locs = findPeakLocation(fAccSig, 0.8 * DSP.max(fAccSig));
        extendList(dominantPeaksOfAcc, w(locs,N,fSampling) );

        fAccSig = absFreqz(accZ);
        locs = findPeakLocation(fAccSig, 0.8 * DSP.max(fAccSig));
        extendList(dominantPeaksOfAcc, w(locs,N,fSampling) ); */

        //double[] fAccSig = absFreqz(accX);
        locs = getLocFromFreqz(accX, strBpm, endBpm, threshold, fSampling, N);  // findPeakLocation(fAccSig, 0.8 * DSP.max(fAccSig));
        extendList(dominantPeaksOfAcc, w(locs,N,strBpm,fSampling) );

        locs = getLocFromFreqz(accY, strBpm, endBpm, threshold, fSampling, N);  // findPeakLocation(fAccSig, 0.8 * DSP.max(fAccSig));
        extendList(dominantPeaksOfAcc, w(locs,N,strBpm,fSampling) );

        locs = getLocFromFreqz(accZ, strBpm, endBpm, threshold, fSampling, N);  // findPeakLocation(fAccSig, 0.8 * DSP.max(fAccSig));
        extendList(dominantPeaksOfAcc, w(locs,N,strBpm,fSampling) );



        double accCloseFreqMax = -1;

        for(int i=0; i < locs.length; i++){
            for(int j=0; j<dominantPeaksOfAcc.size();j++){

                if( Math.abs( peakFreqs[i] - dominantPeaksOfAcc.get(j) ) > 5 ){
                    if( peakFreqs[i] > accCloseFreqMax ){
                        accCloseFreqMax = peakFreqs[i];
                    }
                }
            }
        }

        if( accCloseFreqMax != -1 ){
            fPrev = accCloseFreqMax;
            return fPrev;
        }

        // else return strongest peak location
        // int index = maxIndexOfFreqz(fSig);
        // fPrev = w(locs,N,strBpm,fSampling)[0];
        return maxIndexOfFreqz(sig, strBpm, endBpm, fSampling, N);

    }

    /**
     *
     * @param d
     * @return
     */
    public static int maxIndex(double[] d){

        int index = -1;
        double maxVal = Double.MIN_VALUE;
        for(int i=0;i<d.length;i++){
            if( d[i] > maxVal ){
                index = i;
                maxVal = d[i];
            }
        }

        return index;
    }

    public static double maxVal(double[] d){
        int n = maxIndex(d);
        return d[n];
    }

    /**
     *
     * @param list
     * @param d
     */
    public static void extendList(ArrayList<Double> list, double[] d){
        if (d==null) return;
        for(int i=0;i<d.length;i++){
            list.add(d[i]);
        }
    }

    public static int[] concateArray(int[] a1,int[] a2){
        int[] a = new int[a1.length+a2.length];
        System.arraycopy(a1, 0, a, 0, a1.length);
        System.arraycopy(a2, 0, a, a1.length, a2.length);
        return a;

    }

    /**
     *
     * @param array
     * @param indices
     * @return
     */
    public static double[] getValuesFromIndex(double[] array,int[] indices){

        double[] vals = new double[indices.length];
        for(int i=0; i < indices.length; i++){
            vals[i] = array[indices[i]];
        }
        return vals;
    }

    /**
     *
     * @param sig
     * @param threshold
     * @return
     */
    public static int[] findPeakLocation(double[] sig, double threshold){

        int[] locs = findPeakLocation(sig); // get all local maximum

        ArrayList<Integer> locList = new ArrayList<>();

        double thrVal = threshold ;//* maxVal(sig);

        for (int loc : locs) {
            if( sig[loc] >= thrVal ){
                locList.add(loc);
            }
        }

        return toIntArray(locList);

    }


    public static int[] findPeakLocation( double[] sig,double threshold,double minPeakDistance ){

        int[] locs = findPeakLocation(sig, threshold);
        double[] arr = getValuesFromIndex(sig, locs);
        sort(arr,false);
        ArrayList<Double> list = new ArrayList<>();
        for (int i = 0; i < arr.length; i++) {
            if(arr[i]==-1){
                continue;
            }
            list.add(arr[i]);
            for (int j = i+1; j < arr.length; j++) {
                if(arr[j]==-1)continue;
                if( ( arr[i] - arr[j] ) <= minPeakDistance ){
                    arr[j] = -1;
                }
            }
        }

        // get indices
        int[] indices = new int[list.size()];
        for (int i = 0; i < list.size(); i++) {

            for (int j = 0; j < sig.length; j++) {
                if( sig[j] == arr[i] ){
                    indices[i] = j;
                    break;
                }
            }

        }

        return indices;
    }

    public static int[] sortWithIndex(double[] array,boolean ascend){

        return ArrayIndexComparator.sort(array, ascend);
    }

    public static void sort(double[] arr,boolean ascend ){

        Arrays.sort(arr);
        if(ascend == false){
            reverse(arr);
        }

    }

    public static void reverse(double[] arr){

        int len = arr.length;
        int hlen =  arr.length/2;
        for (int i = 0; i < hlen; i++) {
            double tmp = arr[i];
            arr[i] = arr[len - i - 1];
            arr[len - i - 1] = tmp;

        }
    }

    // TODO:
   /* public static double[] absFreqz(double[] nsig,double[] w ){

        int N = (int) Math.pow(2, 17);
        double[] fr = absFreqz(nsig, N);


        return null;
    }*/


    // TODO:......

   /* public static double[] absFreqz( double[] nsig){


        return null;
    }*/

    public static double[] absFreqz( double[] nsig, int N){

        // if signal length less than N, then zero pad
//        double[] sig;
//        double[] resp = new double[N]; // frequency response
//
//
//        if( nsig.length < N ){
//            sig = zeroPad(nsig,N-nsig.length);
//        }else{
//            sig = nsig;
//        }
//
//
//        double[] re = new double[N];
//        System.arraycopy(sig, 0, re, 0 , N); // re vector initialized with sig
//        double[] imag = DSP.zeroVec(N); // imag vector initialized by zero
//
////        FFT fft = new FFT(N);
////        fft.fft(re, imag);
////
////
////        for (int i = 0; i < N; i++) {
////            resp[i] = Math.sqrt( re[i]*re[i] + imag[i]*imag[i]);
////        }
//
//        resp = FFTbase.fft(re, imag, true);
//
//        return resp;

        return FFTbase.doFFT(nsig, N);

    }

    /**
     *
     * @param b
     * @param s
     * @return
     */
    public static Complex polyVal(double[] b, Complex s ){

        int len = b.length;
        Complex sum = new Complex(0,0);

        for(int i=0; i<len; i++){

            sum = sum.plus( s.pow(i).times( b[i] ) );

        }
        return sum;
    }


    public static double[] linspace(int strt,int end,int len){

        return linspace((double)strt, (double)end, len);

    }

    public static double[] linspace(double strt,double end,int len){
        double step;
        if(1!=len) {
            step = 1.0 * ( end - strt  ) / (len-1);
        }
        else{
            step = 0;
        }
        double[] lin = new double[len];

        double val = strt;
        for(int i=0;i<len;i++){
            lin[i] = val;
            val += step;
        }

        return lin;
    }


    //TODO
    /**
     *
     * @param rSig signal to be filtered
     * @param fSampling sampling frequency
     * @param num
     * @return
     * @throws java.io.FileNotFoundException
     */
    public  double[] myBandPass(double[] rSig, double fSampling,int num) throws IOException {


        double[] b;
        switch (num) {
            case 1:
                b = getArray("filter_coeff/mypass_test.txt");
                break;
            case 2:
                b = getArray("filter_coeff/mypass_test.txt");
                break;
            default:
                b = null;
                break;
        }
        double[] a = {1.0};

        double[] output = DSP.filter(b, a, rSig);

        return output;

    }

    /**
     *
     * @param l
     *       arrayList to convert to double array
     * @return
     */
    public static double[] toDoubleArray(ArrayList<Double> l){
        double [] d = new double[l.size()];

        for(int i=0; i < l.size(); i++){
            d[i] = l.get(i);
        }

        return d;
    }


    public static int[] toIntArray(ArrayList<Integer> l){
        int [] d = new int[l.size()];

        for(int i=0; i < l.size(); i++){
            d[i] = l.get(i);
        }

        return d;
    }



    // TODO:
    /**
     *
     * @param lParameter parameter of RLS filter
     * @param dSig
     * @param mainSig main signal
     * @return filtered signal
     */

    public static double[] rlsFilter(int lParameter,double[] mainSig,double[] dSig){

        int iteration = 5000;
        RLSAlgo rls = new RLSAlgo(lParameter,iteration);
        rls.setXD(mainSig, dSig);
        rls.compute();
        return rls.e();
    }



    /**
     *
     * @param fileName
     * @return ArrayList of data
     * @throws java.io.FileNotFoundException
     */

    /*public static double[] getArray(String fileName) throws IOException {

        AssetManager am = context.getAssets();
        InputStream is = am.open(fileName);
        Scanner scanner = new Scanner(is);

        ArrayList<Double> dataList;
        dataList = new ArrayList<>();
        //System.out.println(scanner.nextLine());
        int tCounter = 0;
        while(scanner.hasNextDouble()){
            dataList.add(scanner.nextDouble());

            tCounter++;
            if(tCounter > 2100)
                break;
        }

        return toDoubleArray(dataList);

    }*/



    public   double[] getArray(String fileName) throws IOException {


        AssetManager assetManager = context.getAssets();

        InputStream fis = null;
        DataInputStream dis = null;
        BufferedReader bf = null;

        double[] value = null;
        ArrayList<Double> d = new ArrayList<>();
        try {
            fis = assetManager.open(fileName);

            bf = new BufferedReader( new InputStreamReader( fis ) );


            int i = 0;
            String line = "";
            while( (line = bf.readLine()) != null) {

                d.add( Double.parseDouble(line) );
                i++;
                if(i > 10000)
                    break;
            }


        } catch (Exception ex) {

            try {
                if(fis != null)
                    fis.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return toDoubleArray(d);

    }

    public static double byteArrayToDouble(byte[] bytes) {
        return ByteBuffer.wrap(bytes).getDouble();
    }


    private static double[] filterArray(ArrayList<Double> fRlsSet, int min, int max) {
        return filterArray( toDoubleArray(fRlsSet) , min, max);
    }

    private static double[] nwem(double[] sigWithNoise, int imfToChose) {

        return Emd.getImf(sigWithNoise, imfToChose);

    }

    public static double maxFind(double[] imf1, double fSampling) {

        int N = (int)Math.pow(2, 17);

        int strBpm = 0;
        int endBpm = 200;

        int[] loc = getLocFromFreqz(imf1, strBpm, endBpm, fSampling, N);



        /*int N = 1024;
        double[] Py = absFreqz(imf1, N); // power spectrum square of imf1
        int loc = maxIndex(Py);

        return 200.0/(1024-1) * (loc - 1);*/

        return w(loc[0], N, strBpm, (float)fSampling);

    }

    private static double[] maxFindFromThreshold(double[] accX, double threshold, double fSampling) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
       /* int N = 4096;
        double[] Py = absFreqz(accX, N); // power spectrum square of imf1
        int[] locs = findPeakLocation(Py, threshold, 3);

        double tmp = 300.0 / (4096 - 1);

        return DSP.times( DSP.minus(locs, 1)  , tmp);*/

        int N = (int)Math.pow(2, 17);

        int strBpm = 0;
        int endBpm = 3000;
        double min = 3;
        int[] loc = getLocFromFreqz(accX, strBpm, endBpm, threshold, min, fSampling, N);

        return w(loc, N, strBpm, (float)fSampling);

    }

    private static double[] timeDomain(double[] sig1, double[] accX, double[] accY, double[] accZ, double fPrev, int multiplier) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        double[] en = DSP.plus( DSP.plus( DSP.power(accX, 2)  , DSP.power(accY, 2)) , DSP.power(accZ, 2));

        double tt = 0.1 * maxVal(en);

        //ArrayList<Integer> bin = new ArrayList<>();

        int[] bin = new int[en.length];

        for (int i = 0; i < en.length; i++) {
            if( en[i] > tt ){
                bin[i] = 1;
            }
            else{
                bin[i] = 0;
            }
        }

        int cl=0;
        int mxl=0;
        int mxst=0;
        int st=1;

        for (int i = 0; i < 200.0*5*multiplier; i++) {
            if(bin[i]==0){
                cl++;
                if(cl>=mxl){
                    mxl = cl;
                    mxst = st;
                }
            }
            else{
                cl = 0;
                st = i + 1;
            }

        }

        double [] y;

        if(mxst+mxl-1<=200*5*multiplier){
            int[] I = DSP.irange(mxst, mxst+mxl-1);

            if(I.length >= 80*5*multiplier){
                int kkk = I[I.length-1];
                I = DSP.irange( (kkk-400*multiplier+1),kkk );
            }

            if( ( I.length>27*5*multiplier && I[I.length-1]>150*5*multiplier ) || I.length>60*5*multiplier ){
                y = new double[I.length];
                for (int i = 0; i < I.length; i++) {
                    y[i] = sig1[I[i]];
                }
            }else{
                y = null;
            }
        }else{
            y = null;
        }

        return y;
    }

    /**
     * find local maximum of sig
     * @param sig
     * @return
     */
    public static int[] findPeakLocation(double[] sig) {
        // throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        ArrayList<Integer> locs = new ArrayList<>();
        int[] locs_;
        int len = sig.length;
        if(len==0)return null;
        else if(len==1 ){
            locs_ = DSP.zeroVecInt(1);
            return locs_;
        };

        for (int i = 1; i < len-1; i++) {

            if( i == 0  ){ // first location
                if( sig[i] > sig[i+1] ){
                    locs.add(i);
                }
            }
            else if(i == len-1) // last location
            {
                if( sig[i]>sig[i-1] ){
                    locs.add(i);
                }
            }
            else{

                if( sig[i] > sig[i-1] && sig[i] >= sig[i+1] ){
                    locs.add(i);
                }

            }

        }

        int[] a = toIntArray(locs);
        return sortWithIndex.sort(sig,a,false);


    }

    public static double[] maxFindFromThreshold(double[] y, double d, int fSampling) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.

        int N = (int)Math.pow(2, 12);
        int strBpm = 0;
        int endBpm = 300;
        int[] loc = getLocFromFreqz(y, strBpm, endBpm, 0.8, fSampling, N);
        return w(loc, N, strBpm, fSampling);

    }

    public static double[] zeroPad(double[] nsig, int i) {
        //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        double[] new_sig = new double[nsig.length+i];
        for (int j = 0; j < new_sig.length ; j++) {
            if(j<nsig.length){
                new_sig[j] = nsig[j];
            }
            else{
                new_sig[j] = 0;
            }
        }
        return new_sig;
    }


}