/*
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 *
 */
package org.apache.qpid.server.security;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InterfaceAddress;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.security.spec.InvalidKeySpecException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.xml.bind.DatatypeConverter;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;

import org.apache.qpid.server.configuration.IllegalConfigurationException;
import org.apache.qpid.server.logging.EventLogger;
import org.apache.qpid.server.logging.messages.KeyStoreMessages;
import org.apache.qpid.server.model.AbstractConfiguredObject;
import org.apache.qpid.server.model.Broker;
import org.apache.qpid.server.model.Content;
import org.apache.qpid.server.model.CustomRestHeaders;
import org.apache.qpid.server.model.IntegrityViolationException;
import org.apache.qpid.server.model.ManagedAttributeField;
import org.apache.qpid.server.model.ManagedObjectFactoryConstructor;
import org.apache.qpid.server.model.Port;
import org.apache.qpid.server.model.RestContentHeader;
import org.apache.qpid.server.model.State;
import org.apache.qpid.server.model.StateTransition;
import org.apache.qpid.transport.network.security.ssl.SSLUtil;
import org.apache.qpid.util.Strings;

public class AutoGeneratedSelfSignedKeyStoreImpl
        extends AbstractConfiguredObject<AutoGeneratedSelfSignedKeyStoreImpl>
        implements AutoGeneratedSelfSignedKeyStore<AutoGeneratedSelfSignedKeyStoreImpl>
{

    private static final SecureRandom RANDOM = new SecureRandom();


    private static Constructor<?> CONSTRUCTOR;
    private static Method GENERATE_METHOD;
    private static Method GET_PRIVATE_KEY_METHOD;
    private static Method GET_SELF_CERTIFICATE_METHOD;
    private static Constructor<?> X500_NAME_CONSTRUCTOR;
    private static Constructor<?> DNS_NAME_CONSTRUCTOR;
    private static Constructor<?> IP_ADDR_NAME_CONSTRUCTOR;
    private static Constructor<?> GENERAL_NAMES_CONSTRUCTOR;
    private static Constructor<?> GENERAL_NAME_CONSTRUCTOR;
    private static Method ADD_NAME_TO_NAMES_METHOD;
    private static Constructor<?> ALT_NAMES_CONSTRUCTOR;
    private static Constructor<?> CERTIFICATE_EXTENSIONS_CONSTRUCTOR;
    private static Method SET_EXTENSION_METHOD;
    private static Method EXTENSION_GET_NAME_METHOD;

    private final Broker<?> _broker;
    private final EventLogger _eventLogger;

    @ManagedAttributeField
    private String _keyAlgorithm;
    @ManagedAttributeField
    private String _signatureAlgorithm;
    @ManagedAttributeField
    private int    _keyLength;
    @ManagedAttributeField
    private int    _durationInMonths;

    private PrivateKey _privateKey;
    private X509Certificate _certificate;
    private KeyManager[] _keyManagers;


    private boolean _generated;
    private boolean _created;


    @ManagedObjectFactoryConstructor(conditionallyAvailable = true)
    public AutoGeneratedSelfSignedKeyStoreImpl(final Map<String, Object> attributes, Broker<?> broker)
    {
        super(parentsMap(broker), attributes);
        _broker = broker;
        _eventLogger = _broker.getEventLogger();
        _eventLogger.message(KeyStoreMessages.CREATE(getName()));
    }

    @Override
    public KeyManager[] getKeyManagers() throws GeneralSecurityException
    {
        return _keyManagers;
    }

    @Override
    public String getKeyAlgorithm()
    {
        return _keyAlgorithm;
    }

    @Override
    public String getSignatureAlgorithm()
    {
        return _signatureAlgorithm;
    }

    @Override
    public int getKeyLength()
    {
        return _keyLength;
    }

    @Override
    public int getDurationInMonths()
    {
        return _durationInMonths;
    }

    @Override
    public String getEncodedCertificate()
    {
        try
        {
            return DatatypeConverter.printBase64Binary(_certificate.getEncoded());
        }
        catch (CertificateEncodingException e)
        {
            throw new IllegalConfigurationException("Cannot encode certificate", e);
        }
    }

    @Override
    public String getEncodedPrivateKey()
    {
        return DatatypeConverter.printBase64Binary(_privateKey.getEncoded());
    }

    @Override
    protected void postResolve()
    {
        super.postResolve();
        if(getActualAttributes().containsKey(ENCODED_PRIVATE_KEY) && getActualAttributes().containsKey(ENCODED_CERTIFICATE))
        {
            loadPrivateKeyAndCertificate();
        }
        else
        {
            generatePrivateKeyAndCertificate();
        }
        generateKeyManagers();

    }

    private void loadPrivateKeyAndCertificate()
    {
        byte[] privateKeyEncoded = Strings.decodeBase64((String) getActualAttributes().get(ENCODED_PRIVATE_KEY));
        byte[] certificateEncoded = Strings.decodeBase64((String) getActualAttributes().get(
                ENCODED_CERTIFICATE));


        try(ByteArrayInputStream input = new ByteArrayInputStream(certificateEncoded))
        {
            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            _certificate = (X509Certificate) cf.generateCertificate(input);
        }
        catch (CertificateException | IOException e)
        {
            throw new IllegalConfigurationException("Could not decode certificate", e);
        }

        try
        {
            _privateKey = SSLUtil.readPrivateKey(privateKeyEncoded, _keyAlgorithm);
        }
        catch (NoSuchAlgorithmException | InvalidKeySpecException e)
        {
            throw new IllegalConfigurationException("Could not decode private key", e);
        }
    }

    @Override
    protected void onCreate()
    {
        super.onCreate();
        _created = true;
    }

    @StateTransition(currentState = { State.UNINITIALIZED, State.STOPPED, State.ERRORED}, desiredState = State.ACTIVE)
    protected ListenableFuture<Void> activate()
    {
        if(!_created)
        {
            saveDerivedAttributesIfNecessary();
        }
        setState(State.ACTIVE);

        return Futures.immediateFuture(null);
    }

    private void saveDerivedAttributesIfNecessary()
    {
        if(_generated)
        {

            final Object encodedCertificate = getEncodedCertificate();
            attributeSet(ENCODED_CERTIFICATE, encodedCertificate, encodedCertificate);

            final Object encodedPrivateKey = getEncodedPrivateKey();
            attributeSet(ENCODED_PRIVATE_KEY, encodedPrivateKey, encodedPrivateKey);

            _generated = false;
        }
    }

    @StateTransition(currentState = {State.UNINITIALIZED, State.ACTIVE, State.ERRORED}, desiredState = State.DELETED)
    protected ListenableFuture<Void> doDelete()
    {
        // verify that it is not in use
        String storeName = getName();

        Collection<Port> ports = new ArrayList<Port>(_broker.getPorts());
        for (Port port : ports)
        {
            if (port.getKeyStore() == this)
            {
                throw new IntegrityViolationException("Key store '"
                                                      + storeName
                                                      + "' can't be deleted as it is in use by a port:"
                                                      + port.getName());
            }
        }
        deleted();
        setState(State.DELETED);
        _eventLogger.message(KeyStoreMessages.DELETE(getName()));
        return Futures.immediateFuture(null);
    }

    private void generatePrivateKeyAndCertificate()
    {
        try
        {
            Object certAndKeyGen = CONSTRUCTOR.newInstance(_keyAlgorithm, _signatureAlgorithm);
            GENERATE_METHOD.invoke(certAndKeyGen, _keyLength);
            _privateKey = (PrivateKey) GET_PRIVATE_KEY_METHOD.invoke(certAndKeyGen);

            Object generalNames = GENERAL_NAMES_CONSTRUCTOR.newInstance();

            Set<InetAddress> addresses = new HashSet<>();
            for (NetworkInterface networkInterface : Collections.list(NetworkInterface.getNetworkInterfaces()))
            {
                for (InterfaceAddress inetAddress : networkInterface.getInterfaceAddresses())
                {
                    addresses.add(inetAddress.getAddress());
                }
            }

            Set<String> dnsNames = new HashSet<>();

            for(InetAddress address : addresses)
            {

                String hostName = address.getHostName();
                if (hostName != null)
                {
                    dnsNames.add(hostName);
                }
                String canonicalHostName = address.getCanonicalHostName();
                if (canonicalHostName != null)
                {
                    dnsNames.add(canonicalHostName);
                }
            }
            for(String dnsName : dnsNames)
            {
                if(dnsName.matches("[\\w&&[^\\d]][\\w\\d.-]*"))
                {
                    ADD_NAME_TO_NAMES_METHOD.invoke(generalNames,
                                                    GENERAL_NAME_CONSTRUCTOR.newInstance(DNS_NAME_CONSTRUCTOR.newInstance(
                                                            dnsName)));
                }
            }

            for(InetAddress inetAddress : addresses)
            {
                ADD_NAME_TO_NAMES_METHOD.invoke(generalNames, GENERAL_NAME_CONSTRUCTOR.newInstance(IP_ADDR_NAME_CONSTRUCTOR.newInstance(inetAddress.getHostAddress())));
            }
            Object altNamesExtension = ALT_NAMES_CONSTRUCTOR.newInstance(generalNames);
            Object certificateExtensions = CERTIFICATE_EXTENSIONS_CONSTRUCTOR.newInstance();
            SET_EXTENSION_METHOD.invoke(certificateExtensions, EXTENSION_GET_NAME_METHOD.invoke(altNamesExtension), altNamesExtension);

            long startTime = System.currentTimeMillis();
            Calendar calendar = Calendar.getInstance();
            calendar.setTimeInMillis(startTime);
            calendar.add(Calendar.MONTH, _durationInMonths);
            long duration = (calendar.getTimeInMillis() - startTime)/1000;

            _certificate = (X509Certificate) GET_SELF_CERTIFICATE_METHOD.invoke(certAndKeyGen, X500_NAME_CONSTRUCTOR.newInstance("CN=Qpid"), new Date(startTime), duration, certificateExtensions);

            _generated = true;

        }
        catch (InstantiationException | IllegalAccessException | InvocationTargetException | IOException e)
        {
            throw new IllegalConfigurationException("Unable to construct keystore", e);
        }
    }

    private void generateKeyManagers()
    {
        try
        {
            X509Certificate[] certs = new X509Certificate[] { _certificate };


            java.security.KeyStore inMemoryKeyStore = java.security.KeyStore.getInstance(java.security.KeyStore.getDefaultType());

            byte[] bytes = new byte[64];
            char[] chars = new char[64];
            RANDOM.nextBytes(bytes);
            StandardCharsets.US_ASCII.decode(ByteBuffer.wrap(bytes)).get(chars);
            inMemoryKeyStore.load(null, chars);
            inMemoryKeyStore.setKeyEntry("1", _privateKey, chars, certs);

            KeyManagerFactory kmf = KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
            kmf.init(inMemoryKeyStore, chars);
            _keyManagers = kmf.getKeyManagers();

        }
        catch (IOException | GeneralSecurityException e)
        {
            throw new IllegalConfigurationException("Cannot load private key or certificate(s): " + e, e);
        }
    }



    static boolean isAvailable()
    {
        try
        {
            Class<?> certAndKeyGenClass;
            try
            {
                certAndKeyGenClass = Class.forName("sun.security.x509.CertAndKeyGen");
            }
            catch (ClassNotFoundException e)
            {
                certAndKeyGenClass = Class.forName("sun.security.tools.keytool.CertAndKeyGen");
            }

            final Class<?> x500NameClass = Class.forName("sun.security.x509.X500Name");
            final Class<?> certificateExtensionsClass = Class.forName("sun.security.x509.CertificateExtensions");
            final Class<?> generalNamesClass = Class.forName("sun.security.x509.GeneralNames");
            final Class<?> generalNameClass = Class.forName("sun.security.x509.GeneralName");
            final Class<?> extensionClass = Class.forName("sun.security.x509.SubjectAlternativeNameExtension");


            CONSTRUCTOR = certAndKeyGenClass.getConstructor(String.class, String.class);
            GENERATE_METHOD = certAndKeyGenClass.getMethod("generate", Integer.TYPE);
            GET_PRIVATE_KEY_METHOD = certAndKeyGenClass.getMethod("getPrivateKey");
            GET_SELF_CERTIFICATE_METHOD = certAndKeyGenClass.getMethod("getSelfCertificate", x500NameClass, Date.class, Long.TYPE,
                                                                       certificateExtensionsClass);
            X500_NAME_CONSTRUCTOR = x500NameClass.getConstructor(String.class);
            DNS_NAME_CONSTRUCTOR = Class.forName("sun.security.x509.DNSName").getConstructor(String.class);
            IP_ADDR_NAME_CONSTRUCTOR = Class.forName("sun.security.x509.IPAddressName").getConstructor(String.class);
            GENERAL_NAMES_CONSTRUCTOR = generalNamesClass.getConstructor();
            GENERAL_NAME_CONSTRUCTOR = generalNameClass.getConstructor(Class.forName("sun.security.x509.GeneralNameInterface"));
            ADD_NAME_TO_NAMES_METHOD =  generalNamesClass.getMethod("add", generalNameClass);
            ALT_NAMES_CONSTRUCTOR = extensionClass.getConstructor(generalNamesClass);
            CERTIFICATE_EXTENSIONS_CONSTRUCTOR = certificateExtensionsClass.getConstructor();
            SET_EXTENSION_METHOD = certificateExtensionsClass.getMethod("set", String.class, Object.class);
            EXTENSION_GET_NAME_METHOD = extensionClass.getMethod("getName");

            return true;
        }
        catch (ClassNotFoundException | LinkageError | NoSuchMethodException e)
        {
            return false;
        }

    }

    @Override
    public void regenerateCertificate()
    {
        generatePrivateKeyAndCertificate();
        saveDerivedAttributesIfNecessary();
    }

    @Override
    public Content getClientTrustStore(String password)
    {

        try
        {
            KeyStore inMemoryKeyStore =
                    KeyStore.getInstance(KeyStore.getDefaultType());

            inMemoryKeyStore.load(null, null);
            inMemoryKeyStore.setCertificateEntry(getName(), _certificate);

            return new TrustStoreContent(inMemoryKeyStore, getName(), password == null ? new char[0] : password.toCharArray());
        }
        catch (CertificateException | NoSuchAlgorithmException | IOException | KeyStoreException e)
        {
            throw new IllegalArgumentException(e);
        }
    }


    @Override
    public Content getCertificate()
    {
        try
        {
            return new CertificateContent(_certificate, getName());
        }
        catch (CertificateEncodingException e)
        {
            throw new IllegalArgumentException("Cannot decode encode the certificate");
        }

    }

    private static class TrustStoreContent implements Content, CustomRestHeaders
    {
        private final KeyStore _keyStore;
        private final char[] _password;
        private final String _disposition;

        public TrustStoreContent(final KeyStore inMemoryKeyStore,
                                 final String name, final char[] password)
        {
            _keyStore = inMemoryKeyStore;
            _password = password;
            _disposition = "attachment; filename=\"" + name + ".jks\"";
        }

        @Override
        public void write(final OutputStream outputStream) throws IOException
        {
            try
            {
                _keyStore.store(outputStream, _password);
            }
            catch (KeyStoreException | NoSuchAlgorithmException | CertificateException e)
            {
                throw new IllegalArgumentException(e);
            }
        }

        @RestContentHeader("Content-Type")
        public String getContentType()
        {
            return "application/octet-stream";
        }

        @RestContentHeader("Content-Disposition")
        public String getContentDisposition()
        {
            return _disposition;
        }

    }

    private static class CertificateContent implements Content, CustomRestHeaders
    {

        private final String _disposition;
        private final String _certString;

        public CertificateContent(final X509Certificate certificate, final String name)
                throws CertificateEncodingException
        {
            _disposition = "attachment; filename=\"" + name + ".pem\"";
            StringBuffer certStringBuffer = new StringBuffer("-----BEGIN CERTIFICATE-----\n");
            String cert = DatatypeConverter.printBase64Binary(certificate.getEncoded());
            int offset = 0;
            while(cert.length()-offset > 64)
            {
                certStringBuffer.append(cert.substring(offset, offset+64));
                offset+=64;
                certStringBuffer.append("\n");
            }
            certStringBuffer.append(cert.substring(offset));
            certStringBuffer.append("\n-----END CERTIFICATE-----\n");
            _certString = certStringBuffer.toString();
        }

        @Override
        public void write(final OutputStream outputStream) throws IOException
        {
            Writer w = new OutputStreamWriter(outputStream);
            w.write(_certString);
            w.flush();
        }

        @RestContentHeader("Content-Type")
        public String getContentType()
        {
            return "text/plain";
        }

        @RestContentHeader("Content-Disposition")
        public String getContentDisposition()
        {
            return _disposition;
        }

    }


}
