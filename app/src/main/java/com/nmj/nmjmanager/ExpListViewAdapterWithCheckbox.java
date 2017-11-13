package com.nmj.nmjmanager;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseExpandableListAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

// Eclipse wanted me to use a sparse array instead of my hashmaps, I just suppressed that suggestion
@SuppressLint("UseSparseArrays")
public class ExpListViewAdapterWithCheckbox extends BaseExpandableListAdapter {

    ImageView lineColorCode;
    // Define activity context
    private Context mContext;

    /*
     * Here we have a Hashmap containing a String key
     * (can be Integer or other type but I was testing
     * with contacts so I used contact name as the key)
    */
    private HashMap<String, List<String>> mListDataChild;

    // ArrayList that is what each key in the above
    // hashmap points to
    private ArrayList<String> mListDataGroup;

    // Hashmap for keeping track of our checkbox check states
    private HashMap<Integer, boolean[]> mChildCheckStates;
    private HashMap<Integer, ImageButton> mGroupResetButton;
    private Boolean[] checkBoxState;

    private HashMap<Integer, HashMap<Integer, CheckBox>> mGroupCheckBoxes;
    private HashMap<Integer, CheckBox> mChildCheckBoxes;

    // Our getChildView & getGroupView use the viewholder patter
    // Here are the viewholders defined, the inner classes are
    // at the bottom
    private ChildViewHolder childViewHolder;
    private GroupViewHolder groupViewHolder;

    /*
          *  For the purpose of this document, I'm only using a single
     *	textview in the group (parent) and child, but you're limited only
     *	by your XML view for each group item :)
    */
    private String groupText;
    private String childText;

    /*  Here's the constructor we'll use to pass in our calling
     *  activity's context, group items, and child items
    */
    public ExpListViewAdapterWithCheckbox(Context context, ArrayList<String> listDataGroup, HashMap<String, List<String>> listDataChild) {

        mContext = context;
        mListDataGroup = listDataGroup;
        mListDataChild = listDataChild;

        // Initialize our hashmap containing our check states here
        mChildCheckStates = new HashMap<>();
        mGroupResetButton = new HashMap<>();

        mGroupCheckBoxes = new HashMap<>();
        mChildCheckBoxes = new HashMap<>();
        checkBoxState = new Boolean[getGroupCount()];
        Arrays.fill(checkBoxState, Boolean.FALSE);
    }

    @Override
    public int getGroupCount() {
        return mListDataGroup.size();
    }

    /*
     * This defaults to "public object getGroup" if you auto import the methods
     * I've always make a point to change it from "object" to whatever item
     * I passed through the constructor
    */
    @Override
    public String getGroup(int groupPosition) {
        return mListDataGroup.get(groupPosition);
    }

    @Override
    public long getGroupId(int groupPosition) {
        return groupPosition;
    }

    @Override
    public View getGroupView(final int groupPosition, boolean isExpanded,
                             View convertView, final ViewGroup parent) {
        //  I passed a text string into an activity holding a getter/setter
        //  which I passed in through "ExpListGroupItems".
        //  Here is where I call the getter to get that text
        groupText = getGroup(groupPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) mContext.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_group, null);

            // Initialize the GroupViewHolder defined at the bottom of this document
            groupViewHolder = new GroupViewHolder();
            groupViewHolder.mGroupText = convertView.findViewById(R.id.lblListHeader);
            groupViewHolder.mGroupImageButton = convertView.findViewById(R.id.resetFilter);

            mGroupResetButton.put(groupPosition, groupViewHolder.mGroupImageButton);
            convertView.setTag(groupViewHolder);
        } else {
            groupViewHolder = (GroupViewHolder) convertView.getTag();
        }

        groupViewHolder.mGroupText.setText(groupText);

        System.out.println("Setting state for Group " + groupPosition + ": " + checkBoxState[groupPosition]);


        groupViewHolder.mGroupImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                boolean getChecked[] = new boolean[getChildrenCount(groupPosition)];
                ;
                for (int i = 0; i < getChildrenCount(groupPosition); i++) {
                    getChecked[i] = false;
                }
                mChildCheckStates.put(groupPosition, getChecked);

                notifyDataSetChanged();

/*                boolean getChecked[];
                if (mChildCheckStates.get(groupPosition) == null)
                    getChecked = new boolean[getChildrenCount(groupPosition)];
                else
                    getChecked = mChildCheckStates.get(groupPosition);
                for (int i = 0; i < getChildrenCount(groupPosition); i++) {
                    if (state == null) {
                        // do nothing
                    }else if (state) {
                        getChecked[i] = true;
                    } else {
                        getChecked[i] = false;
                    }
                }
                checkBoxState[groupPosition] = state;

                mChildCheckStates.put(groupPosition, getChecked);
                System.out.println("Original State: " + state);
                System.out.println("Group Position: " + groupPosition);
                System.out.println("Group State: " + checkBoxState[groupPosition]);

                notifyDataSetChanged();*/
            }
        });
        return convertView;
    }

    @Override
    public int getChildrenCount(int groupPosition) {
        if (mListDataChild.get(mListDataGroup.get(groupPosition)) != null)
            return mListDataChild.get(mListDataGroup.get(groupPosition)).size();
        else return 0;
    }

    /*
     * This defaults to "public object getChild" if you auto import the methods
     * I've always make a point to change it from "object" to whatever item
     * I passed through the constructor
    */
    @Override
    public String getChild(int groupPosition, int childPosition) {
        return mListDataChild.get(mListDataGroup.get(groupPosition)).get(childPosition);
    }

    @Override
    public long getChildId(int groupPosition, int childPosition) {
        return childPosition;
    }


    @Override
    public View getChildView(final int groupPosition, int childPosition, boolean isLastChild, View convertView, final ViewGroup parent) {

        final int mGroupPosition = groupPosition;
        final int mChildPosition = childPosition;

        //  I passed a text string into an activity holding a getter/setter
        //  which I passed in through "ExpListChildItems".
        //  Here is where I call the getter to get that text
        childText = getChild(mGroupPosition, mChildPosition);

        if (convertView == null) {
            LayoutInflater inflater = (LayoutInflater) this.mContext
                    .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
            convertView = inflater.inflate(R.layout.list_item, null);

            childViewHolder = new ChildViewHolder();

            childViewHolder.mChildText = (TextView) convertView
                    .findViewById(R.id.lblListItem);

            childViewHolder.mChildCheckBox = (CheckBox) convertView
                    .findViewById(R.id.lstcheckBox);

            convertView.setTag(R.layout.list_item, childViewHolder);
        } else {
            childViewHolder = (ChildViewHolder) convertView
                    .getTag(R.layout.list_item);
        }
        mChildCheckBoxes.put(childPosition, childViewHolder.mChildCheckBox);
        mGroupCheckBoxes.put(groupPosition, mChildCheckBoxes);
        childViewHolder.mChildText.setText(childText);
        /*
         * You have to set the onCheckChangedListener to null
		 * before restoring check states because each call to
		 * "setChecked" is accompanied by a call to the
		 * onCheckChangedListener
		*/
        childViewHolder.mChildCheckBox.setOnCheckedChangeListener(null);

        if (mChildCheckStates.containsKey(mGroupPosition)) {
            /*
             * if the hashmap mChildCheckStates<Integer, Boolean[]> contains
			 * the value of the parent view (group) of this child (aka, the key),
			 * then retrive the boolean array getChecked[]
			*/
            boolean getChecked[] = mChildCheckStates.get(mGroupPosition);

            // set the check state of this position's checkbox based on the
            // boolean value of getChecked[position]
            childViewHolder.mChildCheckBox.setChecked(getChecked[mChildPosition]);
        } else {
            /*
             * if the hashmap mChildCheckStates<Integer, Boolean[]> does not
			 * contain the value of the parent view (group) of this child (aka, the key),
			 * (aka, the key), then initialize getChecked[] as a new boolean array
			 *  and set; it's size to the total number of children associated with
			 *  the parent group
			*/
            boolean getChecked[] = new boolean[getChildrenCount(mGroupPosition)];

            // add getChecked[] to the mChildCheckStates hashmap using mGroupPosition as the key
            mChildCheckStates.put(mGroupPosition, getChecked);

            // set the check state of this position's checkbox based on the
            // boolean value of getChecked[position]
            childViewHolder.mChildCheckBox.setChecked(false);
        }

        childViewHolder.mChildCheckBox.setOnCheckedChangeListener(new OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                boolean getChecked[] = mChildCheckStates.get(mGroupPosition);
                getChecked[mChildPosition] = isChecked;
                mChildCheckStates.put(mGroupPosition, getChecked);

            }
        });

        return convertView;
    }

    /*
 * Find if all values are checked.
 */
    protected boolean isAllValuesChecked(boolean[] mChecked) {

        for (int i = 0; i < mChecked.length; i++) {
            if (!mChecked[i]) {
                return false;
            }
        }
        return true;
    }

    protected boolean isNotAllValuesChecked(boolean[] mChecked) {

        for (int i = 0; i < mChecked.length; i++) {
            if (mChecked[i]) {
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean isChildSelectable(int groupPosition, int childPosition) {
        return true;
    }

    @Override
    public boolean hasStableIds() {
        return false;
    }

    public CheckBox getChildCheckBox(int childPosition, int groupPosition) {
        return mGroupCheckBoxes.get(groupPosition).get(childPosition);
    }

    public void resetGroupCheckBoxes() {
        for (int i = 0; i < getGroupCount(); i++) {
            boolean[] getChecked = new boolean[getChildrenCount(i)];
            for (int j = 0; j < getChildrenCount(i); j++) {
                getChecked[j] = false;
            }
            mChildCheckStates.put(i, getChecked);
            notifyDataSetChanged();
        }
    }

    public final class GroupViewHolder {

        TextView mGroupText;
        ImageButton mGroupImageButton;
    }

    public final class ChildViewHolder {

        TextView mChildText;
        CheckBox mChildCheckBox;
    }
}