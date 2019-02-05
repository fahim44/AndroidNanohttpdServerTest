package com.fahim.servertest.Calculation;

import java.util.Arrays;
import java.util.Comparator;


public class ArrayIndexComparator implements Comparator<Integer>
{
    private final double[] array;

    public ArrayIndexComparator(double[] array)
    {
        this.array = array;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[array.length];
        for (int i = 0; i < array.length; i++)
        {
            indexes[i] = i; // Autoboxing
        }
        return indexes;
    }

    @Override
    public int compare(Integer index1, Integer index2)
    {
        // Autounbox from Integer to int to use as array indexes

        return Double.compare(array[index1],array[index2]);
    }

    public static void main(String[] args){

        double[] d = {4,2,7,1,10};


        int[] ind = sort(d,false);

        DSP.printArray(ind);

    }

    public static int[] toInt(Integer[] intArray){
        int[] ar = new int[intArray.length];
        for (int i = 0; i < ar.length; i++) {
            ar[i] =intArray[i];
        }
        return ar;
    }

    public static int[] sort(double[] d){
        ArrayIndexComparator comparator = new ArrayIndexComparator( d );
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);
        int[] ind = toInt(indexes);
        return ind;
    }

    public static int[] sort(double[] d,boolean ascend){
        if(ascend){
            return sort(d);
        }else{
            int[] ind = sort(d);
            return reverse(ind);
        }
    }

    public static int[] reverse(int[] arr){
        int[] narr = new int[arr.length];
        int len = arr.length;
        for (int i = 0; i < arr.length; i++) {
            narr[i] = arr[ len - i  - 1 ];

        }
        return narr;
    }
}
