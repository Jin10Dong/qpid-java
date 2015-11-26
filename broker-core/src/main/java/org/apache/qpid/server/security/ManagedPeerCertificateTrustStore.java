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

import java.security.cert.Certificate;
import java.util.List;

import org.apache.qpid.server.model.DerivedAttribute;
import org.apache.qpid.server.model.ManagedAttribute;
import org.apache.qpid.server.model.ManagedObject;
import org.apache.qpid.server.model.ManagedOperation;
import org.apache.qpid.server.model.Param;
import org.apache.qpid.server.model.TrustStore;

@ManagedObject( category = false, type = ManagedPeerCertificateTrustStore.TYPE_NAME)
public interface ManagedPeerCertificateTrustStore<X extends ManagedPeerCertificateTrustStore<X>> extends TrustStore<X>
{

    String TYPE_NAME = "ManagedCertificateStore";


    @ManagedAttribute( defaultValue = "true" )
    boolean isExposedAsMessageSource();

    @ManagedAttribute( oversize = true, defaultValue = "[]" )
    List<Certificate> getStoredCertificates();

    @ManagedOperation
    void addCertificate(@Param(name="certificate") Certificate certificate);

    @DerivedAttribute
    List<CertificateDetails> getCertificateDetails();

    @ManagedOperation
    void removeCertificates(@Param(name="certificates") List<CertificateDetails> certificates);
}
