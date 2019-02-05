package com.fahim.servertest.Calculation;

import java.util.Arrays;
import java.util.Comparator;

/**
 *
 * @author mac
 */
public class sortWithIndex implements Comparator<Integer>{

    private final double[] array;

    private final int[] indices;


    public sortWithIndex(double[] array,int[] indices)
    {
        this.array = array;
        this.indices = indices;
    }

    public Integer[] createIndexArray()
    {
        Integer[] indexes = new Integer[indices.length];
        for (int i = 0; i < indices.length; i++)
        {
            indexes[i] = indices[i]; // Autoboxing
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

        int[] indices = {0,2,4};

        int[] ind = sort(d,indices,false);

        DSP.printArray(ind);

    }

    public static int[] toInt(Integer[] intArray){
        int[] ar = new int[intArray.length];
        for (int i = 0; i < ar.length; i++) {
            ar[i] =intArray[i];
        }
        return ar;
    }

    public static int[] sort(double[] d, int[] indices){
        sortWithIndex comparator = new sortWithIndex(d , indices);
        Integer[] indexes = comparator.createIndexArray();
        Arrays.sort(indexes, comparator);
        int[] ind = toInt(indexes);
        return ind;
    }

    public static int[] sort(double[] d,int[] indices ,boolean ascend){
        if(ascend){
            return sort(d , indices );
        }else{
            int[] ind = sort(d, indices);
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

