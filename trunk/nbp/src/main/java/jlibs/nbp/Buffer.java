/**
 * JLibs: Common Utilities for Java
 * Copyright (C) 2009  Santhosh Kumar T <santhosh.tekuri@gmail.com>
 *
 * This library is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 2.1 of the License, or (at your option) any later version.
 *
 * This library is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 */

package jlibs.nbp;

import java.util.Arrays;

/**
 * @author Santhosh Kumar T
 */
public final class Buffer{
    private char chars[] = new char[100];
    private final Chars data = new Chars(chars, 0, 0);
    private int count;

    private int stack[] = new int[50];
    private int free = 0;

    public boolean isBufferring(){
        return free>0;
    }

    public void push(){
        if(free>=stack.length)
            stack = Arrays.copyOf(stack, 2*stack.length);
        stack[free++] = count;
    }

    private void expandCapacity(int minimumCapacity){
        int newCapacity = (chars.length+1)*2;
        if(newCapacity<0)
            newCapacity = Integer.MAX_VALUE;
        else if(minimumCapacity>newCapacity)
            newCapacity = minimumCapacity;
        chars = Arrays.copyOf(chars, newCapacity);
    }

    public void append(int codePoint){
        int newCount;
        if(codePoint<Character.MIN_SUPPLEMENTARY_CODE_POINT){
            newCount = count+1;
            if(newCount >chars.length)
                expandCapacity(newCount);
            chars[count] = (char)codePoint;
        }else{
            newCount = count+2;
            if(newCount >chars.length)
                expandCapacity(newCount);
            int offset = codePoint - Character.MIN_SUPPLEMENTARY_CODE_POINT;
            chars[count] = (char)((offset >>> 10) + Character.MIN_HIGH_SURROGATE);
            chars[count+1] = (char)((offset & 0x3ff) + Character.MIN_LOW_SURROGATE);
        }
        count = newCount;
    }

    public Chars pop(int begin, int end){
        begin += stack[--free];
        end = count-end;
        data.set(chars, begin, end-begin);
        if(free==0)
            count = 0;
        return data;
    }

    public void clear(){
        free = count = 0;
    }
}
