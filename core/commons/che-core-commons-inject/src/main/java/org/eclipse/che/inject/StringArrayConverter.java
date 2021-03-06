/*
 * Copyright (c) 2012-2018 Red Hat, Inc.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *   Red Hat, Inc. - initial API and implementation
 */
package org.eclipse.che.inject;

import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import com.google.inject.AbstractModule;
import com.google.inject.TypeLiteral;
import com.google.inject.matcher.Matchers;
import com.google.inject.spi.TypeConverter;
import java.util.regex.Pattern;

/** @author andrew00x */
public class StringArrayConverter extends AbstractModule implements TypeConverter {
  private static final Pattern PATTERN = Pattern.compile(" *, *");

  @Override
  public Object convert(String value, TypeLiteral<?> toType) {
    return Iterables.toArray(Splitter.on(PATTERN).split(value), String.class);
  }

  @Override
  protected void configure() {
    convertToTypes(Matchers.only(TypeLiteral.get(String[].class)), this);
  }
}
