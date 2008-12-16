package jlibs.xml.sax;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;
import org.xml.sax.helpers.AttributesImpl;

import javax.xml.namespace.QName;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.Transformer;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.sax.SAXSource;
import java.util.Enumeration;
import java.util.Stack;
import java.io.OutputStreamWriter;

import jlibs.core.lang.StringUtil;
import jlibs.xml.sax.helpers.MyNamespaceSupport;

/**
 * @author Santhosh Kumar T
 */
public abstract class ObjectInputSource<E> extends InputSource{
    private final E obj;

    public ObjectInputSource(E obj){
        this.obj = obj;
    }

    public E getObject(){
        return obj;
    }

    /*-------------------------------------------------[ bootstrap ]---------------------------------------------------*/
    
    private XMLWriter xml;
    void writeInto(XMLWriter xml) throws SAXException{
        this.xml = xml;
        nsSupport.reset();
        attrs.clear();
        elemStack.clear();
        elem = null;
        nsSupport.pushContext();
        
        try{
            xml.startDocument();
            write(obj);
            endElements();
            xml.endDocument();
        }catch(RuntimeException ex){
            throw new SAXException(ex);
        }finally{
            this.xml = null;
        }
    }
    
    protected abstract void write(E obj) throws SAXException;
    
    /*-------------------------------------------------[ Namespaces ]---------------------------------------------------*/

    private MyNamespaceSupport nsSupport = new MyNamespaceSupport();

    public void suggestPrefix(String prefix, String uri){
        nsSupport.suggestPrefix(prefix, uri);
    }

    public String declarePrefix(String uri){
        String prefix = nsSupport.findPrefix(uri);
        if(prefix==null)
            prefix = nsSupport.declarePrefix(uri);
        return prefix;
    }

    public boolean declarePrefix(String prefix, String uri){
        return nsSupport.declarePrefix(prefix, uri);
    }

    private QName declareQName(String uri, String localPart){
        return new QName(uri, localPart, declarePrefix(uri));
    }

    private String toString(String uri, String localPart){
        String prefix = declarePrefix(uri);
        return prefix.length()==0 ? localPart : prefix+':'+localPart;
    }
    
    /*-------------------------------------------------[ start-element ]---------------------------------------------------*/

    private Stack<QName> elemStack = new Stack<QName>();
    private QName elem;
    
    private String toString(QName qname){
        return qname.getPrefix().length()==0 ? qname.getLocalPart() : qname.getPrefix()+':'+qname.getLocalPart();
    }

    private void finishStartElement() throws SAXException{
        if(elem!=null){
            Enumeration enumer = nsSupport.getDeclaredPrefixes();
            while(enumer.hasMoreElements()){
                String prefix = (String) enumer.nextElement();
                xml.startPrefixMapping(prefix, nsSupport.getURI(prefix));
            }
            nsSupport.pushContext();

            elemStack.push(elem);
            xml.startElement(elem.getNamespaceURI(), elem.getLocalPart(), toString(elem), attrs);
            elem = null;
            attrs.clear();
        }
    }

    public ObjectInputSource<E> startElement(String name) throws SAXException{
        finishStartElement();
        return startElement("", name);
    }

    public ObjectInputSource<E> startElement(String uri, String name) throws SAXException{
        finishStartElement();
        elem = declareQName(uri, name);
        return this;
    }

    /*-------------------------------------------------[ Add-Element ]---------------------------------------------------*/

    public ObjectInputSource<E> addElement(String name, String text) throws SAXException{
        return addElement("", name, text);
    }
    
    public ObjectInputSource<E> addElement(String uri, String name, String text) throws SAXException{
        if(!StringUtil.isEmpty(text)){
            startElement(uri, name);
            addText(text);
            endElement();
        }
        return this;
    }

    /*-------------------------------------------------[ Attributes ]---------------------------------------------------*/

    private AttributesImpl attrs = new AttributesImpl();

    public ObjectInputSource<E> addAttribute(String name, String value){
        return addAttribute("", name, value);
    }

    public ObjectInputSource<E> addAttribute(String uri, String name, String value){
        if(!StringUtil.isEmpty(value))
            attrs.addAttribute(uri, name, toString(uri, name), "CDATA", value);
        return this;
    }

    /*-------------------------------------------------[ Text ]---------------------------------------------------*/

    public ObjectInputSource<E> addText(String text) throws SAXException{
        if(!StringUtil.isEmpty(text)){
            finishStartElement();
            xml.characters(text.toCharArray(), 0, text.length());
        }
        return this;
    }

    public ObjectInputSource<E> addCDATA(String text) throws SAXException{
        if(!StringUtil.isEmpty(text)){
            finishStartElement();
            xml.startCDATA();
            xml.characters(text.toCharArray(), 0, text.length());
            xml.endCDATA();
        }
        return this;
    }

    /*-------------------------------------------------[ end-element ]---------------------------------------------------*/

    public ObjectInputSource<E> endElement() throws SAXException{
        finishStartElement();
        QName qname = elemStack.pop();
        xml.endElement(qname.getNamespaceURI(), qname.getLocalPart(), toString(qname));

        Enumeration enumer = nsSupport.getDeclaredPrefixes();
        while(enumer.hasMoreElements())
            xml.endPrefixMapping((String)enumer.nextElement());
        nsSupport.popContext();

        return this;
    }

    public ObjectInputSource<E> endElements() throws SAXException{
        finishStartElement();
        while(!elemStack.isEmpty())
            endElement();
        return this;
    }

    /*-------------------------------------------------[ Errors ]---------------------------------------------------*/

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void warning(String msg, Exception ex) throws SAXException{
        xml.warning(new SAXParseException(msg, null, ex));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void error(String msg, Exception ex) throws SAXException{
        xml.error(new SAXParseException(msg, null, ex));
    }

    @SuppressWarnings({"ThrowableInstanceNeverThrown"})
    public void fatalError(String msg, Exception ex) throws SAXException{
        SAXParseException saxException = new SAXParseException(msg, null, ex);
        xml.fatalError(saxException);
        throw saxException;
    }

    /*-------------------------------------------------[ Others ]---------------------------------------------------*/

    public ObjectInputSource<E> processingInstruction(String target, String data) throws SAXException{
        xml.processingInstruction(target, data);
        return this;
    }

    public ObjectInputSource<E> addComment(String text) throws SAXException{
        if(!StringUtil.isEmpty(text)){
            finishStartElement();
            xml.comment(text.toCharArray(), 0, text.length());
        }
        return this;
    }

    /*-------------------------------------------------[ DTD ]---------------------------------------------------*/

    public ObjectInputSource<E> addPublicDTD(String name, String publicId) throws SAXException{
        xml.startDTD(name, publicId, null);
        xml.endDTD();
        return this;
    }

    public ObjectInputSource<E> addSystemDTD(String name, String systemId) throws SAXException{
        xml.startDTD(name, null, systemId);
        xml.endDTD();
        return this;
    }

    /*-------------------------------------------------[ Test ]---------------------------------------------------*/
    
    public static void main(String[] args) throws Exception{
        InputSource source = new ObjectInputSource<String>(null){
            @Override
            protected void write(String obj) throws SAXException{
                String google = "http://google.com";
                String yahoo = "http://yahoo.com";

                processingInstruction("san", "test='1.2'");

                declarePrefix("google", google);
                declarePrefix("yahoo", yahoo);
                declarePrefix("http://msn.com");
                startElement(google, "hello");
                addAttribute("name", "value");
                addElement("xyz", "helloworld");
                addElement(google, "hai", "test");
                addComment("this is comment");
                addCDATA("this is sample cdata");
            }
        };

        TransformerFactory factory = TransformerFactory.newInstance();
        factory.setAttribute("indent-number", "4");
        Transformer transformer = factory.newTransformer();
        transformer.setOutputProperty(OutputKeys.INDENT, "yes");
        transformer.transform(new SAXSource(new XMLWriter(), source), new StreamResult(new OutputStreamWriter(System.out, "utf-8")));
    }
}
