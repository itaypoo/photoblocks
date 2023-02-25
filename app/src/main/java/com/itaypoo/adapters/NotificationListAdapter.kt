package com.itaypoo.adapters

import android.animation.ObjectAnimator
import android.content.ContentResolver
import android.content.Context
import android.content.res.ColorStateList
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.itaypoo.helpers.Consts
import com.itaypoo.helpers.FirebaseUtils
import com.itaypoo.photoblocks.R
import com.itaypoo.photoblockslib.*

class NotificationListAdapter(private val notifList: MutableList<Notification>,
                              private val context: Context, private val database: FirebaseFirestore,
                              private val contentResolver: ContentResolver) :
RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    // Lambda for item click
    var onBlockCommentClickedListener: ((Block) -> Unit)? = null
    var onBlockInviteClickedListener: ((Block, Notification) -> Unit)? = null
    var onPostLikeClickListener: ((Block) -> Unit)? = null

    // View holder classes
    //region ViewHolders

    class BlockCommentViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val titleText: TextView
        val commenterPicture: ImageView
        val commentText: TextView
        
        init {
            titleText = view.findViewById(R.id.notifCommentItem_titleText)
            commenterPicture = view.findViewById(R.id.notifCommentItem_commenterPicture)
            commentText = view.findViewById(R.id.notifCommentItem_commentText)
        }
    }

    class BlockInviteViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val titleText: TextView
        val blockTitleText: TextView
        val blockImageView: ImageView
        val blockGradient: ImageView
        
        init {
            titleText = view.findViewById(R.id.notifInviteItem_titleText)
            blockTitleText = view.findViewById(R.id.notifInviteItem_blockTitleText)
            blockImageView = view.findViewById(R.id.notifInviteItem_blockImage)
            blockGradient = view.findViewById(R.id.notifInviteItem_blockGradient)
        }
    }

    class PostLikeViewHolder(view: View) : RecyclerView.ViewHolder(view){
        val titleText: TextView
        val postImageView: ImageView
        val likerPicture: ImageView

        init {
            titleText = view.findViewById(R.id.notifPostLikeItem_titleText)
            postImageView = view.findViewById(R.id.notifPostLikeItem_postImage)
            likerPicture = view.findViewById(R.id.notifPostLikeItem_likerPicture)
        }
    }

    //endregion

    // Get the amount of items in the list
    override fun getItemCount(): Int {
        return notifList.size
    }

    // Get the type of this item (notification type)
    override fun getItemViewType(position: Int): Int {
        return notifList[position].type
    }

    // Called when creating the holders. Inflate the holder UI and create its ViewHolder class
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        
        // Choose a layout according to the notification type
        when(viewType){
            NotificationType.BLOCK_COMMENT -> {
                // Notif is block comment
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item_notification_blockcomment, parent, false)
                return BlockCommentViewHolder(view)
            }
            NotificationType.BLOCK_INVITATION -> {
                // Notif is block invitation
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item_notification_blockinvite, parent, false)
                return BlockInviteViewHolder(view)
            }
            NotificationType.POST_LIKE -> {
                // Notif is post like
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.recycler_item_notification_postlike, parent, false)
                return PostLikeViewHolder(view)
            }
            else -> {
                // notification doesn't have a type? return something random
                Log.e("NotificationListAdapter ERROR", "onCreateViewHolder - holder does not have a type!")
                val view = LayoutInflater.from(parent.context).inflate(R.layout.recycler_item_notification_blockinvite, parent, false)
                return CuratedPhotosAdapter.ViewHolder(view)
            }
        }
        
    }

    // Binds a ViewHolder to a specific item in the list
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        val notif = notifList[position]

        holder.itemView.alpha = 0.0f

        // Check which notification this is
        if(holder is BlockCommentViewHolder){
            bindViewHolderBlockComment(holder, notif)
        }
        else if(holder is BlockInviteViewHolder){
            bindViewHolderBlockInvitation(holder, notif)
        }
        else if(holder is PostLikeViewHolder){
            bindViewHolderPostLike(holder, notif)
        }

    }

    // Bind ViewHolder methods
    //region BindViewHolder

    private fun bindViewHolderBlockComment(holder: BlockCommentViewHolder, notif: Notification){
        // Get comment
        database.collection(Consts.DBPath.blockComments).document(notif.content).get().addOnSuccessListener {
            val comment = FirebaseUtils.ObjectFromDoc.BlockComment(it)
            // Get sender
            database.collection(Consts.DBPath.users).document(notif.senderId).get().addOnSuccessListener {
                val sender = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                blockCommentQueriesComplete(holder, comment, sender)
            }
        }
    }

    private fun blockCommentQueriesComplete(holder: BlockCommentViewHolder, comment: BlockComment, sender: User){
        // change the xml text and images
        holder.commentText.text = comment.content
        holder.titleText.text = buildString {
            append(sender.name)
            append(" ")
            append(context.getString(R.string.commented_on_your_block))
        }
        Glide.with(context).load(sender.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(holder.commenterPicture)

        fadeInView(holder.itemView)
    }



    private fun bindViewHolderBlockInvitation(holder: BlockInviteViewHolder, notif: Notification){
        // Get block
        database.collection(Consts.DBPath.blocks).document(notif.content).get().addOnSuccessListener {
            val block = FirebaseUtils.ObjectFromDoc.Block(it)
            // Get inviter user
            database.collection(Consts.DBPath.users).document(notif.senderId).get().addOnSuccessListener {
                val inviter = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                // Bind viewHolder
                blockInvitationQueriesComplete(holder, notif, block, inviter)
            }
        }
    }

    private fun blockInvitationQueriesComplete(holder: BlockInviteViewHolder, notif: Notification, block: Block, inviter: User){
        // change xml text and images
        holder.titleText.text = buildString {
            append(inviter.name)
            append(" ")
            append(context.getString(R.string.notification_block_invite_title))
        }

        holder.blockTitleText.text = block.title
        holder.blockGradient.imageTintList = ColorStateList.valueOf( block.secondaryColor.toInt() )
        Glide.with(context).load(block.coverImageUrl).placeholder(R.drawable.default_block_cover).into(holder.blockImageView)

        fadeInView(holder.itemView)

        // invoke the onClick lambda for this notification when block image view is clicked
        holder.blockImageView.setOnClickListener {
            onBlockInviteClickedListener?.invoke(block, notif)
        }
    }



    private fun bindViewHolderPostLike(holder: PostLikeViewHolder, notif: Notification){
        // Get post
        database.collection(Consts.DBPath.blockPosts).document(notif.content).get().addOnSuccessListener {
            val post = FirebaseUtils.ObjectFromDoc.BlockPost(it)
            // Get sender user
            database.collection(Consts.DBPath.users).document(notif.senderId).get().addOnSuccessListener {
                val sender = FirebaseUtils.ObjectFromDoc.User(it, contentResolver)
                postLikeQueriesComplete(holder, post, sender)
            }
        }
    }

    private fun postLikeQueriesComplete(holder: PostLikeViewHolder, post: BlockPost, sender: User){
        // change xml text and images
        holder.titleText.text = buildString {
            append(sender.name)
            append(" ")
            append(context.getString(R.string.liked_your_post))
        }
        Glide.with(context).load(sender.profilePhotoUrl).placeholder(R.drawable.default_profile_photo).into(holder.likerPicture)
        Glide.with(context).load(post.imageUrl).placeholder(R.drawable.default_block_cover).into(holder.postImageView)

        fadeInView(holder.itemView)
    }

    //endregion

    private fun fadeInView(view: View){
        ObjectAnimator.ofFloat(view, "alpha", 1.0F).apply {
            duration = 300
            interpolator = DecelerateInterpolator()
            start()
        }
    }
}