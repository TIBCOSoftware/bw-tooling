<h1>Visual Diff</h1>
<h2>For TIBCO Business Studio for Business Works.</h2>

Visual Process Diff tool provides an ability to view the changes made to the process file visually between different revisions.

Table of Contents 


* Prerequisite
* Intended Audience
* Applicable BW Version
* Enable visual diff
* Overview of Diff Viewer
* Supported features in 6.6.1 GA
* Modes of usage
* Local History
* Comparing a process from a previous revision from SVN Repository
* Comparing a process from a previous revision from Git Repository


<h4>Prerequisite</h4><br/>
               BW 6.6.1 GA or later.<br/><br/>

<h4>Intended Audience</h4><br/>
		BW Developer, Plug-in Developer.<br/><br/>



Overview of Diff Viewer
After comparing different versions of a process, diff viewer is displayed with decorations to indicate the activities changed and navigation controls to navigate through the modified activities and their modified properties.

![](../../../blob/master/diff-viewer/images/diff_viewer_overview.png)

Navigation :


<table>
<tr>
    <td colspan="2">Activity Navigation Control</td>
  </tr>
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/activity_next.png"/></td>
    <td>Navigate to the next difference.<br/>It navigates to the next Activity which has changed.</td>
  </tr>
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/activity_prev.png"/></td>
    <td>Navigate to the next difference.<br/>It navigates to the next Activity which has changed.</td>
  </tr>  
</table>


While navigating, current activity change is marked by selection indicators and property view for respective activities shown on each side as applicable.

![](../../../blob/master/diff-viewer/images/activity_decoration.png)

<table>
<tr>
    <td colspan="2">Navigating Property Changes</td>
  </tr>
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/property_next.png"/></td>
    <td>Navigate to the next property change.<br/>It navigates to the next property within the current object which has changed.
</td>
  </tr>
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/property_prev.png"/></td>
    <td>Navigate to the previous property change.<br/>It navigates to the previous property within the current object which has changed.</td>
  </tr>  
</table>

1.  All modified properties and their corresponding tabs are shown in blue color to indicate the change.
2.  While navigating through the properties, current property is highlighted in Yellow.

![](../../../blob/master/diff-viewer/images/property_view.png)

As you can see in the screenshot, only “General” and “Input” tabs are decorated as only the properties displayed on these tabs have changed.


<table>
<tr>
    <td colspan="2">Change Indicators</td>
  </tr>
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/activity_added.png"/></td>
    <td>Green “+” indicates a newly added activity.</td>
  </tr>
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/activity_deleted.png"/></td>
    <td>Red “-” indicates deletion of an activity.</td>
  </tr>  
  <tr>
    <td><img src="../../../blob/master/diff-viewer/images/activity_changed.png"/></td>
    <td>Gray “*” indicates changes in one or more properties of that activity.</td>
  </tr>  
</table>



Diff viewer scope in 6.6.1 GA 
You can use the Visual diff tool for comparing :

1.	General and Basic Palettes
2.	Groups
3.	Fault handlers (Catch , CatchAll)
4.	Process / Scope variables (primitive types only)
5.	All process properties.


Modes of usage
There are three ways to see a process diff :

1.	Compare with Local History.
2.	Compare with another revision from an SVN Repository.
3.	Compare with another revision from Git Repository.


Local History
Prerequisite : The user has modified the selected process and saved the file at least once before attempting to compare. This ensures that there is some local history available in the workspace.

1.	Right-click a process and select Compare > Local History.
	![](../../../blob/master/diff-viewer/images/local_history.png)
2.	This opens a History view
3.	Select revisions to be compared
4.	Right-click and select Compare with Each Other.
	![](../../../blob/master/diff-viewer/images/local_history_compare_with_each_other.png)
5.	The TIBCO Business Studio for BusinessWorks(TM) opens a new BW Compare perspective and displays the process diff visually. Changes are not permitted to the process in this perspective. You can go back to the "Design" perspective to modify the process.
![](../../../blob/master/diff-viewer/images/diff_viewer.png)


Comparing a process from a previous revision from SVN Repository

Prerequisite : Project is imported from SVN repository and there are previous revisions of this file available to compare with.

1.	Right-click on a Process.
2.	Go to Team > Show History
	 ![](../../../blob/master/diff-viewer/images/svn_history_view.png)
3.	A History view is displayed.
4.	Select revisions you want to compare.
5.	Right-click > Compare...
        ![](../../../blob/master/diff-viewer/images/svn_history_view.png)
6.	Click OK
        ![](../../../blob/master/diff-viewer/images/svn_compare_with_revision.png)
7.	The TIBCO Business Studio for BusinessWorks(TM) displays the process compare editor and the respective property views.




Comparing a process from a previous revision from Git Repository

Prerequisite : Project is imported from Git repository and there are previous revisions of this file available to compare with.

1.	Right click on a Process.
2.	Go to Team > Show in History
3.	This opens a History view.
4.	Select revisions to be compared.
5.	Right Click and select Compare with Each Other.
        ![](../../../blob/master/diff-viewer/images/git_history_view.png)
6.	The TIBCO Business Studio for BusinessWorks(TM) displays the process compare editor and the respective property views.


Shared Resource Diff : 

Shared resource diff support is added in 6.7.0 GA. It works similar to process diff.
	![](../../../blob/master/diff-viewer/images/SR_Diff.png)



Note : 

1. Namespace registry feature does autopopulate namespace prefixes due to which change is indicated in the input of an Activity,
   but there will be no visible change on the UI. This change may come as a side effect of changing input mapping of any activity in the process.
   This auto population of prefixes can be turned off by updating preference.
   ![](../../../blob/master/diff-viewer/images/namespace_registry_preference.png)

2. In order to turn off visual diff, update the preference.

	1. Go to Windows > Preferences > BusinessWorks > Team Development.
	2. Update the “Enable visual diff” check box and click “Apply”.

![](../../../blob/master/diff-viewer/images/preference.png)

The above preference changes the way process files are compared, comparison of artifacts will work as before 6.6.1 

![](../../../blob/master/diff-viewer/images/xml_xsd_diff.png)   
