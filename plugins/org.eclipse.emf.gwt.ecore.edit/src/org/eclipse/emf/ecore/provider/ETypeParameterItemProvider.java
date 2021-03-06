/**
 * Copyright (c) 2006-2010 IBM Corporation and others.
 * All rights reserved.   This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v20.html
 *
 * Contributors:
 *   IBM - Initial API and implementation
 */
package org.eclipse.emf.ecore.provider;


import java.util.Collection;
import java.util.Iterator;
import java.util.List;

import org.eclipse.emf.common.notify.AdapterFactory;
import org.eclipse.emf.common.notify.Notification;
import org.eclipse.emf.ecore.EGenericType;
import org.eclipse.emf.ecore.EObject;
import org.eclipse.emf.ecore.EStructuralFeature;
import org.eclipse.emf.ecore.ETypeParameter;
import org.eclipse.emf.ecore.EcoreFactory;
import org.eclipse.emf.ecore.EcorePackage;
import org.eclipse.emf.edit.provider.IItemPropertyDescriptor;
import org.eclipse.emf.edit.provider.ViewerNotification;

/**
 * This is the item provider adapter for a {@link org.eclipse.emf.ecore.ETypeParameter} object.
 * <!-- begin-user-doc -->
 * <!-- end-user-doc -->
 * @generated
 */
public class ETypeParameterItemProvider
  extends ENamedElementItemProvider		
{
  /**
   * This constructs an instance from a factory and a notifier.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  public ETypeParameterItemProvider(AdapterFactory adapterFactory)
  {
    super(adapterFactory);
  }

  /**
   * This returns the property descriptors for the adapted class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public List<IItemPropertyDescriptor> getPropertyDescriptors(Object object)
  {
    if (itemPropertyDescriptors == null)
    {
      super.getPropertyDescriptors(object);

    }
    return itemPropertyDescriptors;
  }

  /**
   * This specifies how to implement {@link #getChildren} and is used to deduce an appropriate feature for an
   * {@link org.eclipse.emf.edit.command.AddCommand}, {@link org.eclipse.emf.edit.command.RemoveCommand} or
   * {@link org.eclipse.emf.edit.command.MoveCommand} in {@link #createCommand}.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Collection<? extends EStructuralFeature> getChildrenFeatures(Object object)
  {
    if (childrenFeatures == null)
    {
      super.getChildrenFeatures(object);
      childrenFeatures.add(EcorePackage.Literals.ETYPE_PARAMETER__EBOUNDS);
    }
    return childrenFeatures;
  }
  
  /**
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected EStructuralFeature getChildFeature(Object object, Object child)
  {
    // Check the type of the specified child object and return the proper feature to use for
    // adding (see {@link AddCommand}) it as a child.

    return super.getChildFeature(object, child);
  }

  @Override
  public String getCreateChildText(Object owner, Object feature, Object child, Collection<?> selection)
  {
    return 
      feature == EcorePackage.Literals.ETYPE_PARAMETER__EBOUNDS ?
      getString("_UI_EGenericBoundType_label") :
      super.getCreateChildText(owner, feature, child, selection);
  }

  /**
   * This returns ETypeParameter.gif.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  public Object getImage(Object object)
  {
    return overlayImage(object, getResourceLocator().getImage("full/obj16/ETypeParameter"));
  }

  /**
   * This returns the label text for the adapted class.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
  @Override
  public String getText(Object object)
  {
    return getText((ETypeParameter)object);
  }
  
  static String getText(ETypeParameter eTypeParameter)
  {
    if (eTypeParameter.getEBounds().isEmpty())
    {
      String name = eTypeParameter.getName();
      return name == null ? "null" : name;
    }
    else
    {
      StringBuilder result = new StringBuilder();
      result.append(eTypeParameter.getName());
      result.append(" extends ");
      for (Iterator<EGenericType> i = eTypeParameter.getEBounds().iterator(); i.hasNext(); )
      {
        result.append(EGenericTypeItemProvider.getText(i.next()));
        if (i.hasNext())
        {
          result.append(" & ");
        }
      }
      return result.toString();
    }
  }

  /**
   * This handles model notifications by calling {@link #updateChildren} to update any cached
   * children and by creating a viewer notification, which it passes to {@link #fireNotifyChanged}.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated NOT
   */
  @Override
  public void notifyChanged(Notification notification)
  {
    updateChildren(notification);

    switch (notification.getFeatureID(ETypeParameter.class))
    {
      case EcorePackage.ENAMED_ELEMENT__NAME:
      {
        fireNotifyChanged(new ViewerNotification(notification, ((EObject)notification.getNotifier()).eContainer(), false, true));
        break;
      }
      case EcorePackage.ETYPE_PARAMETER__EBOUNDS:
        fireNotifyChanged(new ViewerNotification(notification, notification.getNotifier(), true, true));
        fireNotifyChanged(new ViewerNotification(notification, ((EObject)notification.getNotifier()).eContainer(), false, true));
        return;
    }
    super.notifyChanged(notification);
  }

  /**
   * This adds {@link org.eclipse.emf.edit.command.CommandParameter}s describing the children
   * that can be created under this object.
   * <!-- begin-user-doc -->
   * <!-- end-user-doc -->
   * @generated
   */
  @Override
  protected void collectNewChildDescriptors(Collection<Object> newChildDescriptors, Object object)
  {
    super.collectNewChildDescriptors(newChildDescriptors, object);

    newChildDescriptors.add
      (createChildParameter
        (EcorePackage.Literals.ETYPE_PARAMETER__EBOUNDS,
         EcoreFactory.eINSTANCE.createEGenericType()));
  }

}
