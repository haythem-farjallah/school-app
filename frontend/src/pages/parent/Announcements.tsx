import React, { useState } from 'react';
import { useTranslation } from 'react-i18next';
import { 
  Megaphone, 
  Search,
  Calendar,
  Bell,
  Eye,
  Filter,
  AlertTriangle
} from 'lucide-react';
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import { Input } from '@/components/ui/input';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { formatDistanceToNow } from 'date-fns';
import { usePublicAnnouncements } from '../../features/announcements/hooks/use-announcements';

const ParentAnnouncements = () => {
  const { t } = useTranslation();
  const [searchQuery, setSearchQuery] = useState('');
  const [importanceFilter, setImportanceFilter] = useState('all');
  
  // API hooks
  const { data: announcementsData, isLoading } = usePublicAnnouncements(0, 50);

  const announcements = announcementsData?.content || [];

  const getPriorityColor = (priority: string) => {
    switch (priority) {
      case 'URGENT':
        return 'bg-red-100 text-red-800 border-red-200';
      case 'HIGH':
        return 'bg-orange-100 text-orange-800 border-orange-200';
      case 'MEDIUM':
        return 'bg-blue-100 text-blue-800 border-blue-200';
      case 'LOW':
        return 'bg-gray-100 text-gray-800 border-gray-200';
      default:
        return 'bg-gray-100 text-gray-800 border-gray-200';
    }
  };

  const filteredAnnouncements = announcements.filter(announcement => {
    const matchesSearch = announcement.title.toLowerCase().includes(searchQuery.toLowerCase()) ||
                         announcement.body.toLowerCase().includes(searchQuery.toLowerCase());
    const matchesImportance = importanceFilter === 'all' || announcement.importance === importanceFilter;
    
    return matchesSearch && matchesImportance;
  });

  return (
    <div className="p-6 space-y-6">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900 flex items-center gap-2">
            <Megaphone className="h-8 w-8 text-blue-600" />
            {t('Announcements')}
          </h1>
          <p className="text-gray-600 mt-1">Stay informed about your child's school</p>
        </div>
      </div>

      {/* Filters */}
      <Card>
        <CardContent className="pt-6">
          <div className="flex flex-col sm:flex-row gap-4">
            <div className="flex-1">
              <div className="relative">
                <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                <Input
                  placeholder="Search announcements..."
                  value={searchQuery}
                  onChange={(e) => setSearchQuery(e.target.value)}
                  className="pl-10"
                />
              </div>
            </div>
            <Select value={importanceFilter} onValueChange={setImportanceFilter}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="Priority" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="all">All Priorities</SelectItem>
                <SelectItem value="URGENT">Urgent</SelectItem>
                <SelectItem value="HIGH">High</SelectItem>
                <SelectItem value="MEDIUM">Medium</SelectItem>
                <SelectItem value="LOW">Low</SelectItem>
              </SelectContent>
            </Select>
          </div>
        </CardContent>
      </Card>

      {/* Announcements List */}
      <div className="space-y-4">
        {isLoading ? (
          <div className="text-center py-8">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-blue-600 mx-auto"></div>
            <p className="text-gray-500 mt-2">Loading announcements...</p>
          </div>
        ) : filteredAnnouncements.length === 0 ? (
          <Card className="text-center py-12">
            <CardContent>
              <Megaphone className="h-12 w-12 text-gray-300 mx-auto mb-4" />
              <h3 className="text-lg font-medium text-gray-900 mb-2">No announcements found</h3>
              <p className="text-gray-500">
                {searchQuery || importanceFilter !== 'all'
                  ? 'Try adjusting your filters to see more results.'
                  : 'No announcements available at this time.'
                }
              </p>
            </CardContent>
          </Card>
        ) : (
          filteredAnnouncements.map((announcement) => (
            <Card key={announcement.id} className={`hover:shadow-lg transition-all duration-200 ${
              announcement.importance === 'URGENT' ? 'border-red-200 bg-red-50/30' : ''
            }`}>
              <CardHeader className="pb-3">
                <div className="flex items-start justify-between">
                  <div className="flex-1">
                    <CardTitle className={`text-lg line-clamp-1 ${
                      announcement.importance === 'URGENT' ? 'text-red-900' : ''
                    }`}>
                      {announcement.title}
                    </CardTitle>
                    <div className="flex items-center gap-2 mt-2">
                      <Badge className={getPriorityColor(announcement.importance)}>
                        {announcement.importance === 'URGENT' && <AlertTriangle className="h-3 w-3 mr-1" />}
                        {announcement.importance}
                      </Badge>
                      <Badge variant="secondary">
                        <Bell className="h-3 w-3 mr-1" />
                        Public
                      </Badge>
                    </div>
                  </div>
                </div>
              </CardHeader>
              
              <CardContent>
                <p className={`line-clamp-4 mb-4 ${
                  announcement.importance === 'URGENT' ? 'text-red-800 font-medium' : 'text-gray-600'
                }`}>
                  {announcement.body}
                </p>
                
                <div className={`flex items-center justify-between text-sm ${
                  announcement.importance === 'URGENT' ? 'text-red-600' : 'text-gray-500'
                }`}>
                  <div className="flex items-center gap-4">
                    <div className="flex items-center gap-1">
                      <Calendar className="h-3 w-3" />
                      {formatDistanceToNow(new Date(announcement.createdAt), { addSuffix: true })}
                    </div>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))
        )}
      </div>
    </div>
  );
};

export default ParentAnnouncements;
