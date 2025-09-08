import * as React from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { Textarea } from "@/components/ui/textarea";
import { Avatar, AvatarFallback } from "@/components/ui/avatar";
import { ScrollArea } from "@/components/ui/scroll-area";
import { Separator } from "@/components/ui/separator";
import { 
  MessageSquare, 
  Send, 
  Heart, 
  Reply, 
  MoreHorizontal,
  Edit,
  Trash2,
  Flag
} from "lucide-react";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { format, formatDistanceToNow } from "date-fns";
import toast from "react-hot-toast";

import type { ResourceComment, CreateResourceCommentRequest } from "@/types/learning-resource";
import { useResourceComments, useCreateResourceComment, useDeleteResourceComment } from "../hooks/use-resource-comments";

interface ResourceCommentsProps {
  resourceId: number;
}

interface CommentItemProps {
  comment: ResourceComment;
  onDelete: (commentId: number) => void;
  onReply?: (comment: ResourceComment) => void;
}

function CommentItem({ comment, onDelete, onReply }: CommentItemProps) {
  const [isLiked, setIsLiked] = React.useState(false);
  const [showReplyForm, setShowReplyForm] = React.useState(false);

  const handleLike = () => {
    setIsLiked(!isLiked);
    toast.success(isLiked ? "Like removed" : "Comment liked");
  };

  const handleDelete = () => {
    if (window.confirm("Are you sure you want to delete this comment?")) {
      onDelete(comment.id);
    }
  };

  const getInitials = (name: string) => {
    return name
      .split(' ')
      .map(word => word[0])
      .join('')
      .toUpperCase()
      .slice(0, 2);
  };

  return (
    <div className="space-y-3">
      <div className="flex items-start space-x-3">
        <Avatar className="h-8 w-8">
          <AvatarFallback className="bg-gradient-to-r from-blue-100 to-indigo-100 text-blue-700 text-xs font-semibold">
            {getInitials(comment.userName)}
          </AvatarFallback>
        </Avatar>
        
        <div className="flex-1 min-w-0">
          <div className="flex items-center justify-between">
            <div className="flex items-center space-x-2">
              <span className="font-medium text-slate-900 text-sm">{comment.userName}</span>
              <span className="text-xs text-slate-500">
                {formatDistanceToNow(new Date(comment.createdAt), { addSuffix: true })}
              </span>
            </div>
            
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button variant="ghost" size="sm" className="h-6 w-6 p-0">
                  <MoreHorizontal className="h-3 w-3" />
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent align="end" className="w-[160px]">
                <DropdownMenuItem onClick={() => onReply?.(comment)}>
                  <Reply className="mr-2 h-3 w-3" />
                  Reply
                </DropdownMenuItem>
                <DropdownMenuItem>
                  <Edit className="mr-2 h-3 w-3" />
                  Edit
                </DropdownMenuItem>
                <DropdownMenuSeparator />
                <DropdownMenuItem>
                  <Flag className="mr-2 h-3 w-3" />
                  Report
                </DropdownMenuItem>
                <DropdownMenuItem 
                  onClick={handleDelete}
                  className="text-red-600 focus:text-red-600"
                >
                  <Trash2 className="mr-2 h-3 w-3" />
                  Delete
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
          
          <p className="text-sm text-slate-700 mt-1 leading-relaxed">
            {comment.content}
          </p>
          
          <div className="flex items-center space-x-4 mt-2">
            <Button
              variant="ghost"
              size="sm"
              onClick={handleLike}
              className={`h-6 px-2 text-xs ${
                isLiked 
                  ? 'text-red-600 hover:text-red-700' 
                  : 'text-slate-500 hover:text-slate-700'
              }`}
            >
              <Heart className={`h-3 w-3 mr-1 ${isLiked ? 'fill-current' : ''}`} />
              {isLiked ? 'Liked' : 'Like'}
            </Button>
            
            <Button
              variant="ghost"
              size="sm"
              onClick={() => setShowReplyForm(!showReplyForm)}
              className="h-6 px-2 text-xs text-slate-500 hover:text-slate-700"
            >
              <Reply className="h-3 w-3 mr-1" />
              Reply
            </Button>
          </div>
        </div>
      </div>
      
      {showReplyForm && (
        <div className="ml-11 mt-3">
          <CommentForm 
            resourceId={comment.resourceId} 
            placeholder="Write a reply..."
            onSuccess={() => setShowReplyForm(false)}
            isReply
          />
        </div>
      )}
    </div>
  );
}

interface CommentFormProps {
  resourceId: number;
  placeholder?: string;
  onSuccess?: () => void;
  isReply?: boolean;
}

function CommentForm({ resourceId, placeholder = "Write a comment...", onSuccess, isReply = false }: CommentFormProps) {
  const [content, setContent] = React.useState("");
  const [isSubmitting, setIsSubmitting] = React.useState(false);
  
  const createCommentMutation = useCreateResourceComment();

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    
    if (!content.trim()) {
      toast.error("Please enter a comment");
      return;
    }

    setIsSubmitting(true);
    
    try {
      await createCommentMutation.mutateAsync({
        content: content.trim(),
        resourceId
      });
      
      setContent("");
      toast.success(isReply ? "Reply posted" : "Comment posted");
      onSuccess?.();
    } catch (error) {
      toast.error("Failed to post comment");
    } finally {
      setIsSubmitting(false);
    }
  };

  return (
    <form onSubmit={handleSubmit} className="space-y-3">
      <Textarea
        value={content}
        onChange={(e) => setContent(e.target.value)}
        placeholder={placeholder}
        className="min-h-[80px] resize-none"
        disabled={isSubmitting}
      />
      <div className="flex items-center justify-between">
        <span className="text-xs text-slate-500">
          {content.length}/500 characters
        </span>
        <div className="flex items-center space-x-2">
          {isReply && (
            <Button
              type="button"
              variant="ghost"
              size="sm"
              onClick={() => onSuccess?.()}
              disabled={isSubmitting}
            >
              Cancel
            </Button>
          )}
          <Button
            type="submit"
            size="sm"
            disabled={!content.trim() || isSubmitting || content.length > 500}
            className="bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-700 hover:to-indigo-700"
          >
            {isSubmitting ? (
              <div className="animate-spin rounded-full h-3 w-3 border-b-2 border-white mr-2"></div>
            ) : (
              <Send className="h-3 w-3 mr-2" />
            )}
            {isReply ? 'Reply' : 'Comment'}
          </Button>
        </div>
      </div>
    </form>
  );
}

export function ResourceComments({ resourceId }: ResourceCommentsProps) {
  const { data: commentsData, isLoading, refetch } = useResourceComments(resourceId);
  const deleteCommentMutation = useDeleteResourceComment();

  const comments = commentsData?.content || [];

  const handleDeleteComment = async (commentId: number) => {
    try {
      await deleteCommentMutation.mutateAsync(commentId);
      toast.success("Comment deleted");
      refetch();
    } catch (error) {
      toast.error("Failed to delete comment");
    }
  };

  const handleReply = (comment: ResourceComment) => {
    // For now, just scroll to the comment form
    toast.info(`Replying to ${comment.userName}`);
  };

  if (isLoading) {
    return (
      <div className="p-6">
        <div className="animate-pulse space-y-4">
          <div className="h-4 bg-slate-200 rounded w-1/4"></div>
          <div className="space-y-3">
            {[1, 2, 3].map((i) => (
              <div key={i} className="flex items-start space-x-3">
                <div className="h-8 w-8 bg-slate-200 rounded-full"></div>
                <div className="flex-1 space-y-2">
                  <div className="h-3 bg-slate-200 rounded w-1/4"></div>
                  <div className="h-4 bg-slate-200 rounded w-3/4"></div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>
    );
  }

  return (
    <div className="flex flex-col h-full">
      <CardHeader className="pb-4">
        <CardTitle className="text-base flex items-center">
          <MessageSquare className="h-4 w-4 mr-2" />
          Comments ({comments.length})
        </CardTitle>
      </CardHeader>
      
      <CardContent className="flex-1 flex flex-col space-y-4">
        {/* Comment Form */}
        <CommentForm 
          resourceId={resourceId} 
          onSuccess={() => refetch()}
        />
        
        <Separator />
        
        {/* Comments List */}
        {comments.length === 0 ? (
          <div className="flex-1 flex items-center justify-center text-center py-8">
            <div>
              <MessageSquare className="h-12 w-12 text-slate-300 mx-auto mb-3" />
              <p className="text-slate-500 text-sm">No comments yet</p>
              <p className="text-slate-400 text-xs">Be the first to share your thoughts!</p>
            </div>
          </div>
        ) : (
          <ScrollArea className="flex-1">
            <div className="space-y-6 pr-4">
              {comments.map((comment) => (
                <CommentItem
                  key={comment.id}
                  comment={comment}
                  onDelete={handleDeleteComment}
                  onReply={handleReply}
                />
              ))}
            </div>
          </ScrollArea>
        )}
      </CardContent>
    </div>
  );
}
