package org.jenkinsci.plugins.casc;

import hudson.model.Describable;
import hudson.model.Descriptor;
import jenkins.model.Jenkins;
import org.jenkinsci.Symbol;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

import static org.jenkinsci.plugins.casc.Configurator.normalize;

/**
 * @author <a href="mailto:nicolas.deloof@gmail.com">Nicolas De Loof</a>
 */
public class DescribableAttribute<Owner, Type> extends Attribute<Owner, Type> {

    public DescribableAttribute(String name, Class<? extends Describable> type) {
        super(name, type);
    }

    @Override
    public List<String> possibleValues() {
        final List<Descriptor> descriptors = Jenkins.getInstance().getDescriptorList(type);
        return descriptors.stream()
                .map(d -> getPreferredSymbol(d, type, d.getKlass().toJavaClass()))
                .collect(Collectors.toList());
    }

    /**
     * Retrieve the preferred symbol for this descriptor
     */
    public static String getPreferredSymbol(Descriptor d, Class extensionPoint, Class target) {
        return getSymbols(d, extensionPoint, target).get(0);
    }


    /**
     * Retrieve all possible symbols for this descriptor, first one being preferred one.
     * If a {@link Symbol} annotation is set, all values are accepted the last one being preferred
     */
    public static List<String> getSymbols(Descriptor d, Class extensionPoint, Class target) {

        if (d != null) {
            List<String> symbols = new ArrayList<>();
            // explicit @Symbol annotation on descriptor
            // last value on descriptor is the preferred one
            // see hudson.slaves.DumbSlave.DescriptorImpl for sample
            Symbol s = d.getClass().getAnnotation(Symbol.class);
            if (s != null) {
                final String[] values = s.value();
                for (int i = values.length-1; i >= 0 ; i--) {
                    symbols.add(values[i]);
                }
            }

            // extension type Foo is implemented as SomeFoo. => "some"
            final String ext = extensionPoint.getSimpleName();
            final String cn = d.getKlass().toJavaClass().getSimpleName();
            if (cn.endsWith(ext)) {
                symbols.add( normalize(cn.substring(0, cn.length() - ext.length())) );
            }

            // extension type Foo is implemented as SomeFooImpl. => "some"
            final String in = extensionPoint.getSimpleName() + "Impl";
            if (cn.endsWith(in)) {
                symbols.add( normalize(cn.substring(0, cn.length() - in.length())) );
            }

            // Fall back to simple class name
            symbols.add( normalize(cn) );
            return symbols;
        }

        // Fall back to simple class name
        return Collections.singletonList(normalize(target.getSimpleName()));

    }



}
