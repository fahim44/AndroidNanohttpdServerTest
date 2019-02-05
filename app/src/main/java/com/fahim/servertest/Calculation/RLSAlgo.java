package com.fahim.servertest.Calculation;

public class RLSAlgo {

    private int nOrder;
    private double lambda;
    private int iteration;
    private double delta;
    private double[] x; // array to filter with rls algorithm
    private double[] weights;
    private double[] d; // desired signal

    public RLSAlgo(int norder,double lambda,int iteration,double delta){
        this.nOrder = norder;
        this.lambda = lambda;
        this.iteration = iteration;
        this.delta = delta;
        this.x = null;
    }


    public RLSAlgo(int norder,int iteration,double delta){
        this(norder,1,iteration,delta);
    }

    public RLSAlgo(int norder,int iteration){
        this(norder,1.0,iteration,0.001);
    }

    public RLSAlgo(int norder){
        this(norder,1.0, -1 ,0.001);
    }

    public void setX(double[] x){
        this.x = x;
        if(this.iteration==-1 || this.iteration > this.x.length){
            this.iteration = this.x.length;
        }
    }

    public void setD(double[] d){
        this.d = d;
    }

    public void setXD(double[] x,double[] d){
        setX(x);
        setD(d);
    }

    public void compute(){

        if( x == null ){
            throw new RuntimeException("Provide x to filter..");
        }

        double[][] P = Matrix.identity(nOrder,1/delta);
        double[] xn;
        double[] zn;
        double den;
        double[] gn;
        double an;

        this.weights = DSP.zeroVec(nOrder);


        for (int i = 0; i < iteration; i++) {



            xn = getXN(x, i, nOrder);

            zn = Matrix.multiply(P, xn);


            den = Matrix.dot(xn, zn);

            den += lambda;

            gn = DSP.times(zn, 1/den);

            an = d[i] - Matrix.dot(this.weights, xn);

            this.weights = DSP.plus( this.weights , DSP.times(gn, an));

            P = Matrix.subtract( P ,  Matrix.multiplyT(gn, zn));

            P = Matrix.multiplyScalar(P, 1/lambda, nOrder);


        }



    }

    public double[] getWeights(){
        return this.weights;
    }

    public double[] e(){
        double[] e = new double[this.x.length];

        for (int i = 0; i < this.x.length; i++) {
            double[] xn = getXN(x, i , this.nOrder);
            e[i] = d[i] - Matrix.dot(xn, this.weights);
        }

        return e;
    }

    public double[] filter(){

        double[] y = new double[this.x.length];

        for (int i = 0; i < this.x.length; i++) {
            double[] xn = getXN(x, i , this.nOrder);
            y[i] = Matrix.dot(xn, this.weights);
        }

        return y;
    }

    // return [ x(n),x(n-1),....,x(n-p+1)]
    public static double[] getXN(double[] x,int n,int p){

        double [] xn = new double[p];

        for (int i = 0; i < p; i++) {

            if(n >= i){
                //////////
                xn[i] = x[n-i];
            }
            else{
                xn[i] = 0;
            }
        }

        return xn;
    }



    public static void main(String[] args){

        double[] x = DSP.range(1, 1000);
        double[] d = DSP.range(6, 1005);

        RLSAlgo rls = new RLSAlgo(30,500);
        rls.setXD(x, d);
        rls.compute();
        DSP.printArray(rls.getWeights(), "Weights ");

        DSP.printArray( rls.filter(),"OUTPUR:" );

    }



}
