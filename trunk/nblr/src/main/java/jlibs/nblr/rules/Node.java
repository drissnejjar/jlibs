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

package jlibs.nblr.rules;

import jlibs.nblr.actions.Action;
import jlibs.xml.sax.SAXProducer;
import jlibs.xml.sax.XMLDocument;
import org.xml.sax.SAXException;

import javax.xml.namespace.QName;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.List;

/**
 * @author Santhosh Kumar T
 */
public class Node implements SAXProducer{
    public int id;
    
    public Action action;
    
    public List<Edge> outgoing = new ArrayList<Edge>();
    public List<Edge> incoming = new ArrayList<Edge>();

    @Override
    public String toString(){
        return action==null ? "" : action.toString();
    }

    public Edge addEdgeTo(Node target){
        return new Edge(this, target);
    }

    public Edge addEdgeFrom(Node source){
        return new Edge(source, this);
    }

    public Edge[] incoming(){
        return incoming.toArray(new Edge[incoming.size()]);
    }

    public Edge[] outgoing(){
        return outgoing.toArray(new Edge[outgoing.size()]);
    }

    /*-------------------------------------------------[ Paths ]---------------------------------------------------*/

    public Paths paths(){
        Paths paths = new Paths();
        travel(new ArrayDeque<Object>(), paths);
        return paths;
    }
    
    private void travel(ArrayDeque<Object> stack, Paths paths){
        if(stack.contains(this))
            throw new IllegalStateException("infinite loop detected!!!");
        else{
            stack.push(this);
            if(outgoing.size()>0){
                for(Edge edge: outgoing()){
                    stack.push(edge);
                    if(edge.matcher!=null){
                        stack.push(edge.target);
                        paths.add(new Path(stack));
                        stack.pop();
                    }else if(edge.rule!=null)
                        edge.rule.node.travel(stack, paths);
                    else
                        edge.target.travel(stack, paths);
                    stack.pop();
                }
            }else
                paths.add(new Path(stack));
            stack.pop();
        }
    }

    /*-------------------------------------------------[ SAXProducer ]---------------------------------------------------*/

    @Override
    public void serializeTo(QName rootElement, XMLDocument xml) throws SAXException{
        xml.startElement("node");
        if(action!=null)
            xml.add(action);
        xml.endElement();
    }

    /*-------------------------------------------------[ Layout ]---------------------------------------------------*/

    public int row;
    public int col;

    public boolean conLeft;
    public boolean conRight;
    public boolean conTop;
    public boolean conBottom;
    
    public int conLeftTop;
    public int conLeftBottom;
    public int conRightTop;
    public int conRightBottom;

    public List<List<Node>> coordinates(){
        List<List<Node>> coordinates = new ArrayList<List<Node>>();
        coordinates(new ArrayList<Node>(), coordinates, this, 0, 0);
        return coordinates;
    }

    private boolean coordinates(List<Node> visited, List<List<Node>> coordinates, Node node, int row, int col){
        if(!visited.contains(node)){
            node.row = row;
            node.col = col;
            visited.add(node);

            while(coordinates.size()<=node.row)
                coordinates.add(new ArrayList<Node>());
            List<Node> rowList = coordinates.get(node.row);
            while(rowList.size()<=node.col)
                rowList.add(null);
            rowList.set(node.col, node);

            col++;
            for(Edge edge: node.outgoing){
                if(coordinates(visited, coordinates, edge.target, row, col))
                    row++;
            }
            return true;
        }else
            return false;
    }
}