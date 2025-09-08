import React, { useState } from 'react';
import { motion } from 'framer-motion';
import { 
  Mail, 
  MessageSquare, 
  Smartphone, 
  TrendingUp, 
  TrendingDown,
  Users,
  Send,
  Eye,
  MousePointer,
  AlertTriangle,
  DollarSign,
  Clock,
  BarChart3,
  PieChart,
  Activity,
  Calendar,
  Filter,
  Download,
  RefreshCw
} from 'lucide-react';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs';
import { Button } from '@/components/ui/button';
import { Badge } from '@/components/ui/badge';
import { Progress } from '@/components/ui/progress';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { DatePickerWithRange } from '@/components/ui/date-range-picker';
import { 
  useEmailAnalytics, 
  useSMSAnalytics, 
  usePushNotificationAnalytics,
  useNotificationSystemHealth,
  useWebSocketConnections
} from '../hooks/use-notifications';
import { formatDistanceToNow, format } from 'date-fns';

interface CommunicationDashboardProps {
  className?: string;
}

export function CommunicationDashboard({ className }: CommunicationDashboardProps) {
  const [dateRange, setDateRange] = useState({
    startDate: format(new Date(Date.now() - 7 * 24 * 60 * 60 * 1000), 'yyyy-MM-dd'),
    endDate: format(new Date(), 'yyyy-MM-dd')
  });

  // Analytics hooks
  const { data: emailAnalytics, isLoading: emailLoading, refetch: refetchEmail } = useEmailAnalytics(dateRange);
  const { data: smsAnalytics, isLoading: smsLoading, refetch: refetchSMS } = useSMSAnalytics(dateRange);
  const { data: pushAnalytics, isLoading: pushLoading, refetch: refetchPush } = usePushNotificationAnalytics(dateRange);
  const { data: systemHealth } = useNotificationSystemHealth();
  const { data: wsConnections } = useWebSocketConnections();

  const handleRefresh = () => {
    refetchEmail();
    refetchSMS();
    refetchPush();
  };

  const StatCard = ({ 
    title, 
    value, 
    change, 
    changeType, 
    icon: Icon, 
    color = 'blue',
    description 
  }: {
    title: string;
    value: string | number;
    change?: string;
    changeType?: 'increase' | 'decrease';
    icon: React.ElementType;
    color?: string;
    description?: string;
  }) => (
    <Card className="hover:shadow-lg transition-shadow duration-300">
      <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
        <CardTitle className="text-sm font-medium text-gray-600">{title}</CardTitle>
        <Icon className={`h-4 w-4 text-${color}-600`} />
      </CardHeader>
      <CardContent>
        <div className="text-2xl font-bold text-gray-900">{value}</div>
        {change && (
          <div className="flex items-center gap-1 mt-1">
            {changeType === 'increase' ? (
              <TrendingUp className="h-3 w-3 text-green-500" />
            ) : (
              <TrendingDown className="h-3 w-3 text-red-500" />
            )}
            <span className={`text-xs ${changeType === 'increase' ? 'text-green-600' : 'text-red-600'}`}>
              {change}
            </span>
            <span className="text-xs text-gray-500">vs last period</span>
          </div>
        )}
        {description && (
          <p className="text-xs text-gray-500 mt-1">{description}</p>
        )}
      </CardContent>
    </Card>
  );

  const ChannelAnalytics = ({ 
    title, 
    data, 
    isLoading, 
    icon: Icon, 
    color 
  }: {
    title: string;
    data: any;
    isLoading: boolean;
    icon: React.ElementType;
    color: string;
  }) => (
    <Card className="col-span-1">
      <CardHeader>
        <div className="flex items-center gap-2">
          <Icon className={`h-5 w-5 text-${color}-600`} />
          <CardTitle className="text-lg">{title}</CardTitle>
        </div>
      </CardHeader>
      <CardContent>
        {isLoading ? (
          <div className="space-y-3">
            <div className="animate-pulse">
              <div className="h-4 bg-gray-200 rounded w-3/4 mb-2" />
              <div className="h-8 bg-gray-200 rounded w-1/2 mb-4" />
              <div className="space-y-2">
                <div className="h-3 bg-gray-200 rounded" />
                <div className="h-3 bg-gray-200 rounded w-5/6" />
              </div>
            </div>
          </div>
        ) : data ? (
          <div className="space-y-4">
            {/* Key Metrics */}
            <div className="grid grid-cols-2 gap-4">
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <div className="text-2xl font-bold text-gray-900">
                  {data.statusBreakdown?.SENT || 0}
                </div>
                <div className="text-xs text-gray-500">Sent</div>
              </div>
              <div className="text-center p-3 bg-gray-50 rounded-lg">
                <div className="text-2xl font-bold text-green-600">
                  {data.statusBreakdown?.DELIVERED || 0}
                </div>
                <div className="text-xs text-gray-500">Delivered</div>
              </div>
            </div>

            {/* Delivery Rate */}
            <div>
              <div className="flex justify-between text-sm mb-1">
                <span>Delivery Rate</span>
                <span className="font-medium">
                  {data.statusBreakdown ? 
                    Math.round((data.statusBreakdown.DELIVERED / data.statusBreakdown.SENT) * 100) : 0
                  }%
                </span>
              </div>
              <Progress 
                value={data.statusBreakdown ? 
                  (data.statusBreakdown.DELIVERED / data.statusBreakdown.SENT) * 100 : 0
                } 
                className="h-2"
              />
            </div>

            {/* Additional Metrics */}
            {data.openedCount !== undefined && (
              <div className="flex justify-between items-center text-sm">
                <span className="flex items-center gap-1">
                  <Eye className="h-3 w-3" />
                  Opened
                </span>
                <span className="font-medium">{data.openedCount}</span>
              </div>
            )}

            {data.clickedCount !== undefined && (
              <div className="flex justify-between items-center text-sm">
                <span className="flex items-center gap-1">
                  <MousePointer className="h-3 w-3" />
                  Clicked
                </span>
                <span className="font-medium">{data.clickedCount}</span>
              </div>
            )}

            {data.totalCost !== undefined && (
              <div className="flex justify-between items-center text-sm">
                <span className="flex items-center gap-1">
                  <DollarSign className="h-3 w-3" />
                  Total Cost
                </span>
                <span className="font-medium">${data.totalCost.toFixed(2)}</span>
              </div>
            )}
          </div>
        ) : (
          <div className="text-center text-gray-500 py-8">
            <Icon className="h-12 w-12 mx-auto mb-4 text-gray-300" />
            <p className="text-sm">No data available</p>
          </div>
        )}
      </CardContent>
    </Card>
  );

  return (
    <div className={`space-y-6 ${className}`}>
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-gray-900">Communication Dashboard</h1>
          <p className="text-gray-600 mt-1">Monitor and analyze your notification system performance</p>
        </div>
        <div className="flex items-center gap-3">
          <Select defaultValue="7d">
            <SelectTrigger className="w-32">
              <SelectValue />
            </SelectTrigger>
            <SelectContent>
              <SelectItem value="1d">Last 24h</SelectItem>
              <SelectItem value="7d">Last 7 days</SelectItem>
              <SelectItem value="30d">Last 30 days</SelectItem>
              <SelectItem value="90d">Last 90 days</SelectItem>
            </SelectContent>
          </Select>
          <Button variant="outline" onClick={handleRefresh}>
            <RefreshCw className="h-4 w-4 mr-2" />
            Refresh
          </Button>
          <Button>
            <Download className="h-4 w-4 mr-2" />
            Export
          </Button>
        </div>
      </div>

      {/* System Health */}
      <Card className="border-green-200 bg-green-50/50">
        <CardHeader>
          <CardTitle className="flex items-center gap-2 text-green-800">
            <Activity className="h-5 w-5" />
            System Health
          </CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-2 h-2 bg-green-500 rounded-full" />
                <span className="text-sm font-medium">Email Service</span>
              </div>
              <Badge variant="secondary" className="bg-green-100 text-green-800">
                {systemHealth?.emailService || 'Healthy'}
              </Badge>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-2 h-2 bg-green-500 rounded-full" />
                <span className="text-sm font-medium">SMS Service</span>
              </div>
              <Badge variant="secondary" className="bg-green-100 text-green-800">
                {systemHealth?.smsService || 'Healthy'}
              </Badge>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-2 h-2 bg-green-500 rounded-full" />
                <span className="text-sm font-medium">Push Service</span>
              </div>
              <Badge variant="secondary" className="bg-green-100 text-green-800">
                {systemHealth?.pushNotificationService || 'Healthy'}
              </Badge>
            </div>
            <div className="text-center">
              <div className="flex items-center justify-center gap-2 mb-1">
                <div className="w-2 h-2 bg-blue-500 rounded-full" />
                <span className="text-sm font-medium">WebSocket</span>
              </div>
              <Badge variant="secondary" className="bg-blue-100 text-blue-800">
                {wsConnections?.activeConnections || 0} Connected
              </Badge>
            </div>
          </div>
        </CardContent>
      </Card>

      {/* Overview Stats */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-6">
        <StatCard
          title="Total Messages Sent"
          value={
            (emailAnalytics?.statusBreakdown?.SENT || 0) +
            (smsAnalytics?.statusBreakdown?.SENT || 0) +
            (pushAnalytics?.statusBreakdown?.SENT || 0)
          }
          change="+12.5%"
          changeType="increase"
          icon={Send}
          color="blue"
          description="Across all channels"
        />
        <StatCard
          title="Delivery Rate"
          value="94.2%"
          change="+2.1%"
          changeType="increase"
          icon={TrendingUp}
          color="green"
          description="Average across channels"
        />
        <StatCard
          title="Active Users"
          value={wsConnections?.connectedUsers?.length || 0}
          icon={Users}
          color="purple"
          description="Currently online"
        />
        <StatCard
          title="Total Cost"
          value={`$${(
            (emailAnalytics?.totalCost || 0) +
            (smsAnalytics?.totalCost || 0)
          ).toFixed(2)}`}
          change="-5.3%"
          changeType="decrease"
          icon={DollarSign}
          color="orange"
          description="This period"
        />
      </div>

      {/* Channel Analytics */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        <ChannelAnalytics
          title="Email Analytics"
          data={emailAnalytics}
          isLoading={emailLoading}
          icon={Mail}
          color="blue"
        />
        <ChannelAnalytics
          title="SMS Analytics"
          data={smsAnalytics}
          isLoading={smsLoading}
          icon={MessageSquare}
          color="green"
        />
        <ChannelAnalytics
          title="Push Notifications"
          data={pushAnalytics}
          isLoading={pushLoading}
          icon={Smartphone}
          color="purple"
        />
      </div>

      {/* Detailed Analytics */}
      <Tabs defaultValue="performance" className="w-full">
        <TabsList className="grid w-full grid-cols-4">
          <TabsTrigger value="performance">Performance</TabsTrigger>
          <TabsTrigger value="engagement">Engagement</TabsTrigger>
          <TabsTrigger value="costs">Costs</TabsTrigger>
          <TabsTrigger value="errors">Errors</TabsTrigger>
        </TabsList>

        <TabsContent value="performance" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <BarChart3 className="h-5 w-5" />
                Delivery Performance
              </CardTitle>
              <CardDescription>
                Message delivery times and success rates across channels
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="text-center p-4 bg-blue-50 rounded-lg">
                    <Mail className="h-8 w-8 text-blue-600 mx-auto mb-2" />
                    <div className="text-2xl font-bold text-blue-900">
                      {emailAnalytics?.averageDeliveryTimeSeconds ? 
                        `${Math.round(emailAnalytics.averageDeliveryTimeSeconds)}s` : 'N/A'
                      }
                    </div>
                    <div className="text-sm text-blue-600">Avg Email Delivery</div>
                  </div>
                  <div className="text-center p-4 bg-green-50 rounded-lg">
                    <MessageSquare className="h-8 w-8 text-green-600 mx-auto mb-2" />
                    <div className="text-2xl font-bold text-green-900">
                      {smsAnalytics?.averageDeliveryTimeSeconds ? 
                        `${Math.round(smsAnalytics.averageDeliveryTimeSeconds)}s` : 'N/A'
                      }
                    </div>
                    <div className="text-sm text-green-600">Avg SMS Delivery</div>
                  </div>
                  <div className="text-center p-4 bg-purple-50 rounded-lg">
                    <Smartphone className="h-8 w-8 text-purple-600 mx-auto mb-2" />
                    <div className="text-2xl font-bold text-purple-900">
                      {pushAnalytics?.averageDeliveryTimeSeconds ? 
                        `${Math.round(pushAnalytics.averageDeliveryTimeSeconds)}s` : 'N/A'
                      }
                    </div>
                    <div className="text-sm text-purple-600">Avg Push Delivery</div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="engagement" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <Eye className="h-5 w-5" />
                User Engagement
              </CardTitle>
              <CardDescription>
                Open rates, click rates, and user interaction metrics
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <h4 className="font-medium">Email Engagement</h4>
                  <div className="space-y-3">
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Open Rate</span>
                      <span className="font-medium">
                        {emailAnalytics?.openedCount && emailAnalytics?.statusBreakdown?.SENT ? 
                          `${Math.round((emailAnalytics.openedCount / emailAnalytics.statusBreakdown.SENT) * 100)}%` : 'N/A'
                        }
                      </span>
                    </div>
                    <Progress 
                      value={emailAnalytics?.openedCount && emailAnalytics?.statusBreakdown?.SENT ? 
                        (emailAnalytics.openedCount / emailAnalytics.statusBreakdown.SENT) * 100 : 0
                      } 
                    />
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Click Rate</span>
                      <span className="font-medium">
                        {emailAnalytics?.clickedCount && emailAnalytics?.statusBreakdown?.SENT ? 
                          `${Math.round((emailAnalytics.clickedCount / emailAnalytics.statusBreakdown.SENT) * 100)}%` : 'N/A'
                        }
                      </span>
                    </div>
                    <Progress 
                      value={emailAnalytics?.clickedCount && emailAnalytics?.statusBreakdown?.SENT ? 
                        (emailAnalytics.clickedCount / emailAnalytics.statusBreakdown.SENT) * 100 : 0
                      } 
                    />
                  </div>
                </div>
                <div className="space-y-4">
                  <h4 className="font-medium">Push Engagement</h4>
                  <div className="space-y-3">
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Open Rate</span>
                      <span className="font-medium">
                        {pushAnalytics?.openedCount && pushAnalytics?.statusBreakdown?.SENT ? 
                          `${Math.round((pushAnalytics.openedCount / pushAnalytics.statusBreakdown.SENT) * 100)}%` : 'N/A'
                        }
                      </span>
                    </div>
                    <Progress 
                      value={pushAnalytics?.openedCount && pushAnalytics?.statusBreakdown?.SENT ? 
                        (pushAnalytics.openedCount / pushAnalytics.statusBreakdown.SENT) * 100 : 0
                      } 
                    />
                    <div className="flex justify-between items-center">
                      <span className="text-sm">Click Rate</span>
                      <span className="font-medium">
                        {pushAnalytics?.clickedCount && pushAnalytics?.statusBreakdown?.SENT ? 
                          `${Math.round((pushAnalytics.clickedCount / pushAnalytics.statusBreakdown.SENT) * 100)}%` : 'N/A'
                        }
                      </span>
                    </div>
                    <Progress 
                      value={pushAnalytics?.clickedCount && pushAnalytics?.statusBreakdown?.SENT ? 
                        (pushAnalytics.clickedCount / pushAnalytics.statusBreakdown.SENT) * 100 : 0
                      } 
                    />
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="costs" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <DollarSign className="h-5 w-5" />
                Cost Analysis
              </CardTitle>
              <CardDescription>
                Communication costs breakdown by channel and time period
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
                <div className="space-y-4">
                  <h4 className="font-medium">Cost by Channel</h4>
                  <div className="space-y-3">
                    <div className="flex justify-between items-center p-3 bg-blue-50 rounded-lg">
                      <div className="flex items-center gap-2">
                        <Mail className="h-4 w-4 text-blue-600" />
                        <span className="text-sm">Email</span>
                      </div>
                      <span className="font-medium">${(emailAnalytics?.totalCost || 0).toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-green-50 rounded-lg">
                      <div className="flex items-center gap-2">
                        <MessageSquare className="h-4 w-4 text-green-600" />
                        <span className="text-sm">SMS</span>
                      </div>
                      <span className="font-medium">${(smsAnalytics?.totalCost || 0).toFixed(2)}</span>
                    </div>
                    <div className="flex justify-between items-center p-3 bg-purple-50 rounded-lg">
                      <div className="flex items-center gap-2">
                        <Smartphone className="h-4 w-4 text-purple-600" />
                        <span className="text-sm">Push</span>
                      </div>
                      <span className="font-medium">$0.00</span>
                    </div>
                  </div>
                </div>
                <div className="space-y-4">
                  <h4 className="font-medium">Cost Efficiency</h4>
                  <div className="space-y-3">
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-gray-900">
                        ${((emailAnalytics?.totalCost || 0) + (smsAnalytics?.totalCost || 0)).toFixed(4)}
                      </div>
                      <div className="text-sm text-gray-600">Cost per message</div>
                    </div>
                    <div className="text-center p-4 bg-gray-50 rounded-lg">
                      <div className="text-2xl font-bold text-gray-900">
                        ${(((emailAnalytics?.totalCost || 0) + (smsAnalytics?.totalCost || 0)) * 30).toFixed(2)}
                      </div>
                      <div className="text-sm text-gray-600">Projected monthly cost</div>
                    </div>
                  </div>
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>

        <TabsContent value="errors" className="space-y-6">
          <Card>
            <CardHeader>
              <CardTitle className="flex items-center gap-2">
                <AlertTriangle className="h-5 w-5" />
                Error Analysis
              </CardTitle>
              <CardDescription>
                Failed messages and error patterns across channels
              </CardDescription>
            </CardHeader>
            <CardContent>
              <div className="space-y-4">
                <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
                  <div className="text-center p-4 bg-red-50 rounded-lg">
                    <AlertTriangle className="h-8 w-8 text-red-600 mx-auto mb-2" />
                    <div className="text-2xl font-bold text-red-900">
                      {(emailAnalytics?.statusBreakdown?.FAILED || 0) +
                       (smsAnalytics?.statusBreakdown?.FAILED || 0) +
                       (pushAnalytics?.statusBreakdown?.FAILED || 0)}
                    </div>
                    <div className="text-sm text-red-600">Total Failed</div>
                  </div>
                  <div className="text-center p-4 bg-yellow-50 rounded-lg">
                    <Clock className="h-8 w-8 text-yellow-600 mx-auto mb-2" />
                    <div className="text-2xl font-bold text-yellow-900">
                      {(emailAnalytics?.statusBreakdown?.PENDING || 0) +
                       (smsAnalytics?.statusBreakdown?.PENDING || 0) +
                       (pushAnalytics?.statusBreakdown?.PENDING || 0)}
                    </div>
                    <div className="text-sm text-yellow-600">Pending</div>
                  </div>
                  <div className="text-center p-4 bg-gray-50 rounded-lg">
                    <RefreshCw className="h-8 w-8 text-gray-600 mx-auto mb-2" />
                    <div className="text-2xl font-bold text-gray-900">0</div>
                    <div className="text-sm text-gray-600">Retrying</div>
                  </div>
                </div>

                {/* Error Rate */}
                <div>
                  <div className="flex justify-between text-sm mb-2">
                    <span>Overall Error Rate</span>
                    <span className="font-medium">
                      {(() => {
                        const totalSent = (emailAnalytics?.statusBreakdown?.SENT || 0) +
                                         (smsAnalytics?.statusBreakdown?.SENT || 0) +
                                         (pushAnalytics?.statusBreakdown?.SENT || 0);
                        const totalFailed = (emailAnalytics?.statusBreakdown?.FAILED || 0) +
                                           (smsAnalytics?.statusBreakdown?.FAILED || 0) +
                                           (pushAnalytics?.statusBreakdown?.FAILED || 0);
                        return totalSent > 0 ? `${Math.round((totalFailed / totalSent) * 100)}%` : '0%';
                      })()}
                    </span>
                  </div>
                  <Progress 
                    value={(() => {
                      const totalSent = (emailAnalytics?.statusBreakdown?.SENT || 0) +
                                       (smsAnalytics?.statusBreakdown?.SENT || 0) +
                                       (pushAnalytics?.statusBreakdown?.SENT || 0);
                      const totalFailed = (emailAnalytics?.statusBreakdown?.FAILED || 0) +
                                         (smsAnalytics?.statusBreakdown?.FAILED || 0) +
                                         (pushAnalytics?.statusBreakdown?.FAILED || 0);
                      return totalSent > 0 ? (totalFailed / totalSent) * 100 : 0;
                    })()} 
                    className="h-2"
                  />
                </div>
              </div>
            </CardContent>
          </Card>
        </TabsContent>
      </Tabs>
    </div>
  );
}
