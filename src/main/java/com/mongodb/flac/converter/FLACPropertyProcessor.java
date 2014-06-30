package com.mongodb.flac.converter;

import com.mongodb.flac.converter.FLACAnnotationException;
import com.mongodb.flac.converter.FLACPropertyProvider;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * FLACPropertyProcessor will process a class that is a FLACPropertyProvider, looking for all
 * getter-Methods that are Annotated with the FLACProperty Annotation and pull out the values
 * of the user permissions that are specified by annotations.
 *
 * <p> The annotation related classes found in package com.mongodb.flac.converter are not
 *     essential to the usage of FLAC but you might find them useful in porting to legacy code.
 * </p>
 *
 * <p> Assume you have some class that knows the attributes that the user has, here:
 *  <pre>
 *             final UserManagementClass cValue = new UserManagementClass("c_sl_value", Arrays.asList("TK"), Arrays.asList("US"));
 *             //        the key thing is the 2nd and 3rd args give the Citizenship and  Sci values
 *
 *             // The following statement will incorporate those values into the   UserSecurityAttributesMapCapco
 *             // class:
 *             Map<String, Object> actualAnnotatedValues = FLACPropertyProcessor.findMethodsAnnotatedPullOutSLFieldInfo(cValue);
 *             UserSecurityAttributesMapCapco userSecurityAttributesMap = new UserSecurityAttributesMapCapco(actualAnnotatedValues);
 *
 *             Assert.assertEquals(Arrays.asList("US"), userSecurityAttributesMap.getCitizenship() );
 *             Assert.assertEquals(Arrays.asList("TK"), userSecurityAttributesMap.getSci() );
 *
 *
 *  </pre>
 *
 *  See test code in com.mongodb.flac.converter.FLACPropertyProcessorTest#testUsingPropProcToInject()
 *  for a complete working version of the above code.
 *
 * </p>
 *
 */
public class FLACPropertyProcessor {

    /**
     * find all getter-Methods that are Annotated with the FLACProperty Annotation and pull out the values
     * @param classInstanceWithAnnotation   where some getters have been labeled with FLACProperty Annotation
     * @return  a map like {c=c_sl_value, relto=[US], sci=[TK]}
     *
     * @see FLACProperty
     */
    public static Map<String, Object> findMethodsAnnotatedPullOutSLFieldInfo(FLACPropertyProvider classInstanceWithAnnotation) throws FLACAnnotationException {

        Map<String, Object> attrAndValueMap = new HashMap<String, Object>();

        //Load provided on the command line class
        Class loadedClass = classInstanceWithAnnotation.getClass();

        // Get references to class methods
        Method[] methods = loadedClass.getMethods();

        // Check every method of the class.If the annotation is present,
        // print the values of its parameters

        for (Method m : methods) {
            if (m.isAnnotationPresent(FLACProperty.class)) {
                FLACProperty flacAnnotation =
                        m.getAnnotation(FLACProperty.class);
                try {
                    Object invokeOutput = m.invoke(classInstanceWithAnnotation);    // runs method w/ Annotation
                    //System.out.printf("m.invoke: %s", invokeOutput);
                    attrAndValueMap.put(flacAnnotation.attributeNameInSl(), invokeOutput);
                } catch (IllegalAccessException e) {
                    throw new FLACAnnotationException("processing " + flacAnnotation.attributeNameInSl(), e);
                } catch (InvocationTargetException e) {
                    throw new FLACAnnotationException("processing " + flacAnnotation.attributeNameInSl(), e);
                }
            }
        }

        return attrAndValueMap;

    }



}



